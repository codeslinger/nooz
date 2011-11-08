(ns nooz.db
  (:require [rn.clorine :as cl]
            [nooz.migrations :as migrations])
  (:use (clojure.contrib
         [sql :only (insert-values
                     delete-rows
                     do-commands
                     create-table
                     drop-table
                     transaction
                     with-query-results)]
         [logging :only (info warn)]
         [core :only (.?.)]
         [java-utils :only (as-str)]))
  (:import (java.sql SQLException)))

(defn- execute-migration [direction]
  (fn [[version {migration-fn direction
                 doc :doc]]
       (info (str (direction {:up "Applying migration "
                              :down "Undoing migration "})
                  version " " doc))
       (transaction
        (migration-fn)
        (if (= :up direction)
          (insert-values :SchemaVersion
                         [:version]
                         [version])
          (delete-rows :SchemaVersion ["version=?" version])))))

(defn- run-migrations [direction from to]
  (dorun (map (execute-migration direction)
              (if (= :up direction)
                (take (- to from) (nthnext migrations/migrations from))
                (reverse (take (- from to) (nthnext migrations/migrations to)))))))

(defn- create-schema-table-if-needed [direction to]
  (try
   (info "Attempting to create SchemaVersion table")
   (create-table :SchemaVersion [:version :int "NOT NULL UNIQUE"])
   (info "No SchemaVersion table exists - first run installing migrations")
   (try
    (run-migrations direction 0 to)
    (catch Exception ex
      (warn "Error running migrations: " ex)))
   (catch Exception e
     (when-not (= java.sql.BatchUpdateException (.?. e getCause getClass))
       (throw (SQLException. "Unknown error whilst creating SchemaVersion table" e))))))

(defn migrate
  "Pass it :up or :down and a version to which to migrate. If no arguments are supplied, we assume application of all migrations."
  ([] (migrate :up (count migrations)))
  ([direction to]
     (cl/with-connection :main
       (when (= :up direction)
         (create-schema-table-if-needed direction to))
       (let [current-version (or (with-query-results rs
                                   ["SELECT MAX(version) AS version FROM SchemaVersion"]
                                   (:version (first rs)))
                                 0)]
         (run-migrations direction current-version to)))))

(defn connect-db [info]
  (cl/register-connection! :main info))
