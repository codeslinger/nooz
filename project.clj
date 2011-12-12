(defproject nooz "0.1.0-SNAPSHOT"
            :description "Collaborative news site"
            ;; :local-repo-classpath true
            :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]]
            :dependencies [[org.clojure/clojure "1.2.1"]
                           [org.clojure/clojure-contrib "1.2.0"]
                           [ring "1.0.0-RC1"]
                           [noir "1.2.1"]
                           [clj-time "0.3.2"]
                           [org.mindrot/jbcrypt "0.3m"]
                           [com.draines/postal "1.6.0"]
                           [org.clojars.tavisrudd/redis-clojure "1.3.0"]]
            :main nooz.server)
