(defproject nooz "0.1.0-SNAPSHOT"
            :description "Collaborative news site"
            :local-repo-classpath true
            :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]]
            :dependencies [[org.clojure/clojure "1.2.1"]
                           [org.clojure/clojure-contrib "1.2.0"]
                           [org.mindrot/jbcrypt "0.3m"]
                           [postgresql "9.1-901.jdbc4"]
                           [korma "0.2.1"]
                           [noir "1.2.1"]
                           [clj-time "0.3.2"]]
            :main nooz.server)
