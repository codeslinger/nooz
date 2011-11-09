(ns nooz.migrations
  (:use [clojure.contrib [sql :only [create-table drop-table]]]))

(def migrations
     (sorted-map
      20111108113207 {:doc "create users table"
                      :up #(create-table :users
                                         [:id "BIGSERIAL"]
                                         [:username "VARCHAR(64)"]
                                         [:password "VARCHAR(20)"]
                                         [:email "VARCHAR(256)"]
                                         [:created_at "TIMESTAMP"]
                                         [:verified_at "TIMESTAMP"])
                      :down #(drop-table :users)
                      }
      20111108113600 {:doc "create posts table"
                      :up #(create-table :posts
                                         [:id "BIGSERIAL"]
                                         [:url "VARCHAR(1024)"]
                                         [:submitted_by "BIGINT"]
                                         [:submitted_at "TIMESTAMP"])
                      :down #(drop-table :posts)
                      }
      20111109095705 {:doc "create replies table"
                      :up #(create-table :replies
                                         [:id "BIGSERIAL"]
                                         [:url "VARCHAR(1024)"]
                                         [:submitted_by "BIGINT"]
                                         [:submitted_at "TIMESTAMP"])
                      :down #(drop-table :replies)
                      }
      20111109100323 {:doc "create comments table"
                      :up #(create-table :comments
                                         [:id "BIGSERIAL"]
                                         [:comment "VARCHAR(1000)"]
                                         [:submitted_by "BIGINT"]
                                         [:submitted_at "TIMESTAMP"])
                      :down #(drop-table :comments)
                      }
      ))
