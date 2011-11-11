(ns nooz.migrations
  (:use [clojure.contrib [sql :only [create-table drop-table]]]))

(def migrations
     (sorted-map
      20111108113207 {:doc "create users table"
                      :up #(create-table :users
                                         [:id "BIGSERIAL"]
                                         [:username "VARCHAR(64) NOT NULL"]
                                         [:password "VARCHAR(64) NOT NULL"]
                                         [:email "VARCHAR(256) NOT NULL"]
                                         [:created_at "TIMESTAMP NOT NULL"]
                                         [:verified_at "TIMESTAMP"])
                      :down #(drop-table :users)
                      }
      20111108113600 {:doc "create posts table"
                      :up #(create-table :posts
                                         [:id "BIGSERIAL"]
                                         [:title "VARCHAR(256) NOT NULL"]
                                         [:url "VARCHAR(1024) NOT NULL"]
                                         [:user_id "BIGINT NOT NULL"]
                                         [:created_at "TIMESTAMP NOT NULL"]
                                         [:expires_at "TIMESTAMP NOT NULL"])
                      :down #(drop-table :posts)
                      }
      20111109095705 {:doc "create replies table"
                      :up #(create-table :replies
                                         [:id "BIGSERIAL"]
                                         [:url "VARCHAR(1024) NOT NULL"]
                                         [:post_id "BIGINT NOT NULL"]
                                         [:user_id "BIGINT NOT NULL"]
                                         [:created_at "TIMESTAMP NOT NULL"])
                      :down #(drop-table :replies)
                      }
      20111109100323 {:doc "create comments table"
                      :up #(create-table :comments
                                         [:id "BIGSERIAL"]
                                         [:comment "VARCHAR(1000) NOT NULL"]
                                         [:post_id "BIGINT NOT NULL"]
                                         [:user_id "BIGINT NOT NULL"]
                                         [:created_at "TIMESTAMP NOT NULL"])
                      :down #(drop-table :comments)
                      }
      ))
