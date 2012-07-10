(defproject nooz "0.1.0-SNAPSHOT"
            :description "Collaborative news site"
            ;; :local-repo-classpath true
            :dev-dependencies [[swank-clojure "1.4.2"]]
            :dependencies [[org.clojure/clojure "1.4.0"]
                           [clj-stacktrace "0.2.4"]
                           [ring "1.1.1"]
                           [noir "1.2.2"]
                           [clj-time "0.4.3"]
                           [org.mindrot/jbcrypt "0.3m"]
                           [com.draines/postal "1.8.0"]
                           [org.clojars.tavisrudd/redis-clojure "1.3.1"]]
            :main nooz.server)
