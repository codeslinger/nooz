(ns nooz.migrations)

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
