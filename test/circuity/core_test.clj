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

(cc/defcommand plus
  [a b]
  (+ a b))

(deftest test-plus-functional
  (is (= 42 (plus 1 41))))

(cc/defcommand sieve-of-primes
  "Prime numbers less than n"
  {:timeout 100 :trip_threshold 2}
  [n]
  (if (<= n 2)
    []
    (let [m (Math/round (Math/sqrt n))]
      (loop [primes []
             [x & numbers] (range 2 n)]
        (cond
          (nil? x) primes
          (> x m) (concat primes [x] numbers)
          :else
          (recur (conj primes x) (remove #(= 0 (mod % x)) numbers)))))))

(deftest test-sieve-of-primes
  (testing "it find prime numbers"
    (is (= [] (sieve-of-primes 1)))
    (is (= [] (sieve-of-primes 2)))
    (is (= [2] (sieve-of-primes 3)))
    (is (= [2 3] (sieve-of-primes 4)))
    (is (= [2 3 5 7 11] (sieve-of-primes 12))))
  (testing "it timeout on big numbers"
    (try
      (sieve-of-primes Long/MAX_VALUE)
      (catch Exception ex
        (is (= :Timeout (:cause (ex-data ex)))))))
  (testing "it trips circuit after reach trip threshold"
    (try
      (sieve-of-primes Long/MAX_VALUE)
      (catch Exception ex
        (is (= :Timeout (:cause (ex-data ex))))))
    (try
      (sieve-of-primes Long/MAX_VALUE)
      (catch Exception ex
        (is (= :CircuitOpen (:cause (ex-data ex))))))
    (try
      (sieve-of-primes 12)
      (catch Exception ex
        (is (= :CircuitOpen (:cause (ex-data ex))))))))


