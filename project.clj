(defproject nooz "0.1.0-SNAPSHOT"
            :description "Collaborative news site"
            :local-repo-classpath true
            :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]]
            :dependencies [[org.clojure/clojure "1.2.1"]
                           [org.clojure/clojure-contrib "1.2.0"]
                           [postgresql "9.1-901.jdbc4"]
                           [com.relaynetwork/clorine "1.0.6-SNAPSHOT"]
                           [noir "1.2.1"]]
            :main nooz.server)
