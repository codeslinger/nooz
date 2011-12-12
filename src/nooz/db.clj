(ns nooz.db)

(def prod-redis {:host "127.0.0.1"
                 :port 6379
                 :db 0
                 :timeout 5000})

(defn symbolize-map [m]
  (apply hash-map (flatten
                   (map #(vector (symbol (nth %1 0)) (nth %1 1)) (seq m)))))
