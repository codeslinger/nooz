(ns nooz.db
  (:require [clojure.string :as s]))

(def prod-redis {:host "127.0.0.1"
                 :port 6379
                 :db 0
                 :timeout 5000})

(defn keywordize-map [m]
  (apply hash-map
         (flatten (map #(let [[k v] %1]
                          (vector (keyword (s/replace k ":" "")) v))
                       (seq m)))))

