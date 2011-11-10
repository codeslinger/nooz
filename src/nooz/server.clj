(ns nooz.server
  (:require [noir.server :as server]
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
  (let [cfg @server-config]
    (swap! server-config assoc :server (server/start (:port cfg) (:mode cfg)))))

(defn setup-db []
    (db/connect-db {:driver-class-name "org.postgresql.Driver"
                    :user "nooz"
                    :password "nooz"
                    :url "jdbc:postgresql://localhost:5432/nooz"
                    :max-active 4})
    (db/migrate))

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (setup-db)
    (swap! server-config assoc :port port :mode mode)
    (restart-server!)))

(comment
  (stop-server!)
  server-config
  (swap! server-config assoc :port 8081)
  (restart-server!)
  (-main :dev)
  (db/migrate :down 0)
  (db/migrate)
  )