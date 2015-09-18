(ns clj-bucket.core
  (:require [clojure.core.async :refer [chan close! <!! <! >! go go-loop dropping-buffer put!]]))

; As observed in https://github.com/brunoV/throttler, the minimum
; reliable sleep period is 10 ms.
(def min-drip-period 10)
(def unit->ms
  {:millisecond 1
   :second 1000
   :minute 60000
   :hour 3600000
   :day 86400000
   :month 2678400000})

(defn- round [n] (Math/round (double n)))

(defn- drip-config
  "Calculates the drip period in ms, given the desired rate.
  If this is less than 10 ms, takes 10 ms instead."
  [rate unit]
  (when-not (unit->ms unit)
    (throw (IllegalArgumentException. (str "Invalid time unit. Available units are: " (keys unit->ms)))))
  (when-not (and rate (pos? rate))
    (throw (IllegalArgumentException. "Rate must be a positive number.")))

  (let [unit (unit->ms unit)
        drip-period (max min-drip-period (int (/ unit rate)))
        rate-ms (/ rate unit)
        tokens-per-drip (round (* drip-period rate-ms))]
    [drip-period tokens-per-drip]))

(defn- bucket-chan
  [size]
  (chan (dropping-buffer size)))

(defn- put-tokens
  [ch n]
  (go-loop [n n
            success? true]
    (if (and (pos? n) success?)
      (recur (dec n) (>! ch :token))
      success?)))

(defn- start-dripping!
  [ch drip-period tokens-per-drip]
  (go
   (while (<! (put-tokens ch tokens-per-drip))
     (Thread/sleep drip-period))))

(defn bucket
  "Creates a new token bucket with the given token drip rate and capacity."
  [size rate unit]
  (when-not (pos? size)
    (throw (IllegalArgumentException. "Bucket size must be a positive number.")))

  (let [bucket (bucket-chan size)
        [drip-period tokens-per-drip] (drip-config rate unit)]
    (start-dripping! bucket drip-period tokens-per-drip)
    bucket))

(defn close-bucket!
  "Closes the bucket channel. A convenience method to help avoid importing clojure.core.async
  in caller namespace."
  [bucket]
  (when bucket
    (close! bucket)))

(defn throttle
  "Throttles the function call with the provided bucket.
  The function call will be made immediately if the bucket has been kicked."
  [bucket f & args]
  (if (<!! bucket)
    (apply f args)
    (throw (IllegalArgumentException. "Bucket has been closed."))))
