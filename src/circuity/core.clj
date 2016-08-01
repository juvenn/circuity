(ns circuity.core)

(defn open?
  "Detect if given circuit state open *now*."
  [{:keys [failure_count trip_threshold
           last_failure reset_window]
    :or {failure_count 0 last_failure 0}}]
  (and (>= failure_count trip_threshold)
       (<= (- (System/currentTimeMillis) last_failure)
           reset_window)))

(defn reset
  "Reset circuit state."
  [circuit]
  (assoc circuit :failure_count 0))

(defn record-failure
  "Record a failure in circuit state."
  [circuit]
  (-> circuit
      (update :failure_count inc)
      (assoc :last_failure (System/currentTimeMillis))))

(defmacro defcommand
  "
  Same as defn but define a function guarded by circuit breaker. It
  supports optional doc-string, and optional attr-map to customize
  circuit behavior such as:

  ```
  (defcommand sleep
    \"Sleep for some microseconds and returns that number of ms\"
    {:timeout 300 :trip_threshold 5 :reset_window (* 10 300)}
    [ms]
    (Thread/sleep ms)
    ms)
  ```

  Invocations on command will be short-circuited (circuit open) if
  failed more than 5 times in the window of 3000 ms, i.e. the command
  will not be executed at all, and exception being thrown right
  away. Until reset window passed, then command will be executed on
  invocation.
  "
  [fname & fdecl]
  (let [[pre-args [args & body]] (split-with (comp not vector?) fdecl)
        attr-map (loop [[head & tail] pre-args]
                   (if (or (nil? head)
                           (map? head))
                     head
                     (recur tail)))
        timeout (get attr-map :timeout 5000)
        trip-threshold (get attr-map :trip_threshold 5)
        reset-window (get attr-map :reset_window (* 10 timeout))]
    `(do
       (let [circuit# (atom {:timeout ~timeout
                             :trip_threshold ~trip-threshold
                             :failure_count 0
                             :last_failure 0
                             :reset_window ~reset-window})]
         (defn ~fname ~@pre-args ~args
           (let [f# (fn [] ~@body)]
             (if-not (open? @circuit#)
               (let [fut# (future (f#))
                     val# (deref fut# ~timeout ::timeout)]
                 (if (not= ::timeout val#)
                   (do
                     (swap! circuit# reset)
                     val#)
                   (do
                     (swap! circuit# record-failure)
                     (throw (ex-info (format "%s failed (timedout) in %s (ms)"
                                             (var ~fname) ~timeout)
                                     {:cause :TimeOut
                                      :circuit @circuit#})))))
               (throw (ex-info (format "%s short-circuited as it failed too often"
                                       (var ~fname))
                               {:cause :CircuitOpen
                                :circuit @circuit#})))))
         (alter-meta! (var ~fname) assoc :circuit circuit#)))))

