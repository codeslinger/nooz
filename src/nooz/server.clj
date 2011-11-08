(ns nooz.server
  (:require [noir.server :as server]
            [rn.clorine :as cl]
            [clojure.contrib.sql :as sql]
            [nooz.db :as db]))

(def server-config
     (atom {:mode :dev
            :port (Integer. (get (System/getenv) "PORT" "8080"))
            :server nil}))

(defn stop-server! []
  (when-not (nil? (@server-config :server))
    (server/stop (@server-config :server))))

(defn restart-server! []
  (stop-server!)
  (server/load-views "src/nooz/views/")
  (swap! server-config
         assoc
         :server (server/start (@server-config :port) (@server-config :mode))))

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (db/connect-db {:driver-class-name "org.postgresql.Driver"
                    :user "nooz"
                    :password "nooz"
                    :url "jdbc:postgresql://localhost:5432/nooz"
                    :max-active 4})
    (db/migrate)
    (swap! server-config assoc :port port :mode mode)
    (restart-server!)))

(comment
  (stop-server!)
  server-config
  (restart-server!)
  (-main :dev))