(ns nooz.db
  (:require [nooz.migrations :as migrations]
            [nooz.crypto :as crypto])
  (:use [clojure.contrib
         [sql :only [insert-values
                     delete-rows
                     do-commands
                     create-table
                     drop-table
                     transaction
                     with-connection
                     with-query-results]]
         [logging :only [info warn]]
         [core :only [.?.]]
         [java-utils :only [as-str]]]
        [korma.db]
        [korma.core])
  (:import (java.sql SQLException)))

(defdb prod
  (postgres {:db "nooz"
             :host "localhost"
             :port "5432"
             :user "nooz"
             :password "nooz"}))

(defentity comments)
(defentity replies)
(defentity posts)
(defentity users
  (prepare
   (fn [{password :password :as user}]
     (assoc user :password (crypto/gen-hash password))))
  (has-many posts)
  (has-many comments)
  (has-many replies))

(defn- execute-migration [direction]
  (fn [[version {migration-fn direction
                 doc          :doc}]]
       (info (str (direction {:up "Applying migration "
                              :down "Undoing migration "})
                  version " " doc))
       (transaction
        (migration-fn)
        (if (= :up direction)
          (insert-values :schema_versions
                         [:version]
                         [version])
          (delete-rows :schema_versions ["version=?" version])))))

(defn- run-migrations [direction from to]
  (dorun
   (map (execute-migration direction)
        (if (= :up direction)
          (take (- to from) (nthnext migrations/migrations from))
          (reverse (take (- from to) (nthnext migrations/migrations to)))))))

(defn- create-schema-table-if-needed [direction to]
  (let [version-exists (or (with-query-results rs
                               ["SELECT 1 AS exists FROM information_schema.tables WHERE table_type='BASE TABLE' AND table_name='schema_versions'"]
                             (:exists (first rs)))
                           0)]
    (if (= version-exists 0)
      (try
       (info "Attempting to create schema_versions table")
       (create-table :schema_versions [:version "BIGINT NOT NULL UNIQUE"])
       (info "No schema_versions table exists - first run installing migrations")
       (try
        (run-migrations direction 0 to)
        (catch Exception ex
          (warn "Error running migrations: " ex)))
       (catch Exception e
         (when-not (= java.sql.BatchUpdateException (.?. e getCause getClass))
           (throw (SQLException. "Unknown error whilst creating schema_versions table" e))))))))

(defn migrate
  "Pass it :up or :down and a version to which to migrate. If no arguments are supplied, we assume application of all migrations."
  ([]
     (migrate :up (last (keys migrations/migrations))))
  ([direction to]
     (with-connection (get-connection prod)
       (when (= :up direction)
         (create-schema-table-if-needed direction to))
       (let [current-version (or (with-query-results rs
                                     ["SELECT MAX(version) AS version FROM schema_versions"]
                                   (:version (first rs)))
                                 0)]
         (run-migrations direction current-version to)))))
