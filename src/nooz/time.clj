(ns nooz.time
  (:require [clojure.contrib.math :as math]
            [clj-time.core :as core]
            [clj-time.coerce :as coerce]
            [clj-time.format :as format])
  (:import java.sql.Timestamp))

(defn to-sql [t]
  (Timestamp. t))

(defn long-date [t]
  (coerce/to-long (coerce/from-date t)))

(defn as-long [date]
  (coerce/to-long date))

(defn now []
  (core/now))

(defn time-ago-in-words [from to]
  (let [diff (/ (- to from) 1000 60)]
    (cond
      (< diff 1) "less than a minute"
      (< diff 44) (str (math/round diff) " minutes")
      (< diff 89) "one hour"
      (< diff 1439) (str (math/round (/ diff 60.0)) " hours")
      (< diff 2519) "one day"
      (< diff 43199) (str (math/round (/ diff 1440.0)) " days")
      (< diff 86399) "one month"
      (< diff 525599) (str (math/round (/ diff 43200.0)) " months")
      :else "many months")))

(defn human-time [date]
  (time-ago-in-words (long-date date) (as-long (now))))
