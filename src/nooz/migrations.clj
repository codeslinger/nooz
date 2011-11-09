(ns nooz.migrations
  (:use [clojure.contrib
         [sql :only [insert-values
                     delete-rows
                     do-commands
                     create-table
                     drop-table
                     transaction
                     with-query-results]]
         [logging :only [info warn]]
         [core :only [.?.]]
         [java-utils :only [as-str]]]))

(def migrations
     (sorted-map
      20111108113207 {:doc "create users table"
                      :up #(create-table :users
                                         [:id "SERIAL"]
                                         [:username "VARCHAR(64)"]
                                         [:password "VARCHAR(20)"]
                                         [:email "VARCHAR(256)"])
                      :down #(drop-table :users)
                      }
      20111108113600 {:doc "create posts table"
                      :up #(create-table :posts
                                         [:id "SERIAL"]
                                         [:url "VARCHAR(1024)"]
                                         [:submitted_at "TIMESTAMP"])
                      :down #(drop-table :posts)
                      }
      ))
