(ns nooz.db
  (:require [clojure.string :as s]
            [redis.core :as redis]))

(def prod-redis {:host "127.0.0.1"
                 :port 6379
                 :db 0
                 :timeout 5000})

(defn keywordize-map [m]
  (apply hash-map
         (flatten (map #(let [[k v] %1]
                          (vector (keyword (s/replace k ":" "")) v))
                       (seq m)))))

(defn record-obj! [key obj]
  (let [args (flatten (cons key (seq obj)))]
    (apply redis/hmset args)))
