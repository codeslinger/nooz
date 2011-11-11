(ns nooz.server
  (:require [noir.server :as server]
            [nooz.db :as db])
  (:use ring.middleware.session.cookie))

(def *secret-key* "DEADBEEFCAFEMOOP")

(def server-config
     (atom {:mode :dev
            :port (Integer. (get (System/getenv) "PORT" "8080"))
            :server nil}))

(defn stop-server! []
  (when-not (nil? (@server-config :server))
    (server/stop (@server-config :server))))

(defn start-server! []
  (let [cfg @server-config]
    (server/start
     (:port cfg)
     {:mode (:mode cfg)
      :session-store (cookie-store {:key *secret-key*})
      :session-cookie-attrs {:max-age 1209600
                             :http-only true}})))

(defn restart-server! []
  (stop-server!)
  (server/load-views "src/nooz/views/")
  (swap! server-config assoc :server (start-server!)))

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (db/migrate)
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