(ns nooz.server
  (:require [noir.server :as server]
            [nooz.constants :as const]
            [nooz.db :as db])
  (:use ring.middleware.session.cookie))

(def server-config
     (atom {:mode :dev
            :port (Integer. (get (System/getenv) "PORT" "8080"))
            :server nil}))

(defn stop-server! []
  (when-not (nil? (@server-config :server))
    (server/stop (@server-config :server))))

(defn start-server! []
  (let [cfg @server-config]
    (server/start (:port cfg)
                  {:mode (:mode cfg)
                   :session-store (cookie-store {:key const/secret-key})
                   :session-cookie-attrs {:max-age const/max-session-age-seconds
                                          :http-only true}})))

(defn restart-server! []
  (stop-server!)
  (server/load-views "src/nooz/views/")
  (swap! server-config assoc :server (start-server!)))

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (swap! server-config assoc :port port :mode mode)
    (restart-server!)))

(comment
  (stop-server!)
  (restart-server!)
  (-main :dev)
  server-config
  (swap! server-config assoc :port 8081)
  )