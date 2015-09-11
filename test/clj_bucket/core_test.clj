(ns clj-bucket.core_test
  (:require [clojure.test :refer :all]
            [clj-bucket.core :refer :all]))

(defmacro time-expr
  [times expr]
  `(let [start# (System/nanoTime)
         results# (dotimes [x# ~times] ~expr)
         end# (System/nanoTime)
         duration-ns# (- end# start#)
         duration-s# (/ duration-ns# 1E9)]
     [results# duration-s#]))

(deftest throttling
  (let [bucket (bucket 1 1 :second)
        f (constantly 1)
        results (time-expr 4 (throttle bucket f))]
    (testing "it is not possible to execute N calls in N-1 seconds"
      (is (< 3 (second results))))))
