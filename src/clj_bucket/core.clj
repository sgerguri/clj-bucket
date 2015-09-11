(ns clj-bucket.core
  (:require [clojure.core.async :refer [chan <!! >! <! timeout go dropping-buffer]]))

; To keep the throttler precise even for high frequencies, we set up a
; minimum sleep time. This is an observation originally made in the
; throttler library (TODO: link) that is reused here as well.
(def min-drip-period 10)
(def unit->ms
  {:millisecond 1
   :second 1000
   :minute 60000
   :hour 3600000
   :day 86400000
   :month 2678400000})

(defn- round [n] (Math/round (double n)))

(defn- drip-period
  "Calculates the drip period in ms, given the desired rate.
  If this is less than 10 ms, takes 10 ms instead."
  [rate unit]
  (when-not (unit->ms unit)
    (throw (IllegalArgumentException. (str "Invalid time unit. Available units are: " (keys unit->ms)))))
  (when-not (and rate (pos? rate))
    (throw (IllegalArgumentException. "Rate must be a positive number.")))

  (let [desired-rate (/ (unit->ms unit) rate)]
    (int (round (max desired-rate min-drip-period)))))

(defn- bucket-chan
  [size]
  (chan (dropping-buffer size)))

(defn- start-dripping!
  [ch drip-period]
  (go
    (while (>! ch :token)
      (<! (timeout drip-period)))))

(defn bucket
  "Creates a new token bucket with the given token drip rate and capacity."
  [size rate unit]
  (when-not (pos? size)
    (throw (IllegalArgumentException. "Bucket size must be a positive number.")))

  (let [bucket (bucket-chan size)
        drip-period (drip-period rate unit)]
    (start-dripping! bucket drip-period)
    bucket))

(defn throttle
  "Throttles the function call with the provided bucket.
  The function call will be made immediately if the bucket has been kicked."
  [bucket f & args]
  (if (<!! bucket)
    (apply f args)
    (throw (IllegalArgumentException. "Bucket has been closed."))))