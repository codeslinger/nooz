(ns nooz.db
  (:use [clojure.contrib
         [sql :only [create-table
                     transaction
                     with-connection
                     with-query-results]]
         [logging :only [info warn]]
         [core :only [.?.]]
         [java-utils :only [as-str]]]
        [korma.db]
        [korma.core])
  (:require [nooz.migrations :as migrations])
  (:import (java.sql SQLException
                     BatchUpdateException)))

(defdb prod
  (postgres {:db "nooz"
             :host "localhost"
             :port "5432"
             :user "nooz"
             :password "nooz"}))

(defentity schema_versions)
(defentity comments)
(defentity replies)
(defentity posts)
(defentity users
  (has-many posts)
  (has-many comments)
  (has-many replies))

(defn schema-table-exists? []
  (with-query-results
      rs
      ["SELECT 1 AS exists
          FROM information_schema.tables
         WHERE table_type='BASE TABLE'
           AND table_name='schema_versions'"]
    (:exists (first rs))))

(defn- create-schema-table! []
  (try
   (info "Attempting to create schema_versions table")
   (create-table :schema_versions [:version "BIGINT NOT NULL UNIQUE"])
   (catch Exception e
     (when-not (= BatchUpdateException (.?. e getCause getClass))
       (throw (SQLException. "Error whilst creating schema_versions table" e))))))

(defn- applied-versions []
  (map #(:version %) (select schema_versions)))

(defn- latest-version []
  (last (keys migrations/migrations)))

(defn- apply-migration! [[version {migration-fn :up
                                   doc :doc}]]
  (info (str "Applying migration " version ": " doc))
  (migration-fn)
  (insert schema_versions (values {:version version})))

(defn- revert-migration! [[version {migration-fn :down
                                    doc :doc}]]
  (transaction
    (info (str "Reverting migration " version ": " doc))
    (migration-fn)
    (delete schema_versions (where {:version version}))))

(defn- make-can-apply [to]
  (let [not-applied (complement (set (applied-versions)))]
    (fn [[version _]]
      (and (<= version to)
           (not-applied version)))))

(defn- make-can-revert [to]
  (let [applied (set (applied-versions))]
    (fn [[version _]]
      (and (> version to)
           (applied version)))))

(defn- apply-migrations! [to]
  (let [can-apply (make-can-apply to)]
    (doseq [to-apply (filter can-apply migrations/migrations)]
      (println to-apply)
      (apply-migration! to-apply))))

(defn- revert-migrations! [to]
  (let [can-revert (make-can-revert to)]
    (doseq [to-revert (reverse (filter can-revert migrations/migrations))]
      (revert-migration! to-revert))))

(defn- in-transaction [& body]
  (with-connection (get-connection prod)
    (transaction
      body)))

(defn- up! [to]
  (in-transaction
   (if-not (schema-table-exists?)
     (create-schema-table!))
   (apply-migrations! to)))

(defn- down! [to]
  (in-transaction
   (if (schema-table-exists?)
     (revert-migrations! to))))

(defn migrate!
  "Pass it either no args to migrate up to the latest schema version, or
   a :up or :down and target version number to migrate up or down to."
  ([]
     (migrate! :up (latest-version)))
  ([direction to]
     (if (= :up direction)
       (up! to)
       (down! to))))

(comment
  (migrate!)
  (migrate! :down 0)
  )
