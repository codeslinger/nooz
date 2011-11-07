(ns nooz.server
  (:require [noir.server :as server]))


(def server-config (atom {:mode :dev
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
    (swap! server-config assoc :port port :mode mode)
    (restart-server!)))


(comment
  (stop-server!)

  server-config

  (restart-server!)

  (-main :dev)

)