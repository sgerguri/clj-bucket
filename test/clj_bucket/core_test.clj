(ns clj-bucket.core_test
  (:require [clojure.test :refer :all]
            [clojure.algo.generic.math-functions :refer [approx=]]
            [clj-bucket.core :refer :all]))

(defmacro time-expr
  [times expr]
  `(let [start# (System/nanoTime)
         results# (dotimes [x# ~times] ~expr)
         end# (System/nanoTime)
         duration-ns# (- end# start#)
         duration-s# (Math/round (/ duration-ns# 1E9))]
     [results# duration-s#]))

(deftest throttling
  (let [bucket (bucket 1 1 :second)
        f (constantly 1)
        approx= (fn [expected actual] (approx= expected actual 1E-4))
        results (time-expr 3 (throttle bucket f))]
    (is (< 3 (second results)))))
