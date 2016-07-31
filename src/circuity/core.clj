(ns circuity.core)

(defn deref-future
  "Same as `clojure.core/deref-future`, except it won't suppress exception."
  ([^java.util.concurrent.Future fut]
     (.get fut))
  ([^java.util.concurrent.Future fut timeout-ms]
   (.get fut timeout-ms java.util.concurrent.TimeUnit/MILLISECONDS)))

(defn open?
  "Detect if given circuit state open *now*."
  [{:keys [failure_count trip_threshold
           last_failure reset_window] :as state}]
  (and (>= failure_count trip_threshold)
       (<= (- (System/currentTimeMillis) last_failure)
           reset_window)))

(defn record-fail
  "Record a failure in circuit state."
  [circuit]
  (-> circuit
      (update :failure_count inc)
      (assoc :last_failure (System/currentTimeMillis))))

(defmacro defcommand [fname & fdecl]
  (let [[pre-args [args & body]] (split-with (comp not vector?) fdecl)
        attr-map (loop [[head & tail] pre-args]
                   (if (or (nil? head)
                           (map? head))
                     head
                     (recur tail)))
        timeout (get attr-map :timeout 5000)
        trip-threshold (get attr-map :trip_threshold 5)]
    `(do
       (let [circuit# (atom {:timeout ~timeout
                             :trip_threshold ~trip-threshold
                             :failure_count 0
                             :last_failure 0})]
         (defn ~fname ~@pre-args ~args
           (let [f# (fn [] ~@body)]
             (if-not (open? @circuit#)
               (let [fut# (future (f#))]
                 (try
                   (deref-future fut# ~timeout)
                   (catch Exception ex#
                     (swap! circuit# record-fail)
                     (throw ex#))))
               (throw (ex-info "Circuit open as it fails too often")))))
         (alter-meta! (var ~fname) assoc :circuit circuit#)))))

