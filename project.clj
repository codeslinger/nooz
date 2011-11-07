(defproject nooz "0.1.0-SNAPSHOT"
            :description "Collaborative news site"
            :local-repo-classpath true
            :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]]
            :dependencies [[org.clojure/clojure "1.3.0"]
                           [noir "1.2.1"]]
            :main nooz.server)
