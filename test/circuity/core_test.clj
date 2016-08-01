(ns circuity.core-test
  (:require [clojure.test :refer :all]
            [circuity.core :as cc]))

(deftest test-circuit-open?
  (testing "it's closed if failures do not reach threshold"
    (is (false? (cc/open? {:timeout 5000 :trip_threshold 5
                           :reset_window 5000})))
    (is (false? (cc/open? {:timeout 5000 :trip_threshold 5
                           :last_failure (System/currentTimeMillis)
                           :failure_count 4
                           :reset_window 5000}))))
  (testing "it's open if failures reach threshold and in the reset window"
    (is (true? (cc/open? {:timeout 5000 :trip_threshold 5
                          :last_failure (System/currentTimeMillis)
                          :failure_count 5
                          :reset_window 5000}))))
  (testing "it's closed (half-open) if out of reset window"
    (is (false? (cc/open? {:timeout 5000 :trip_threshold 5
                           :last_failure (- (System/currentTimeMillis) 5001)
                           :failure_count 5
                           :reset_window 5000})))))

(cc/defcommand sleep
  "Sleep for some time and returns ms it has slept"
  {:timeout 300
   :trip_threshold 2
   :reset_window 3000}
  [ms]
  (Thread/sleep ms)
  ms)

