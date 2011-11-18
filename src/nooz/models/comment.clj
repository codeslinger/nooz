(ns nooz.models.comment
  (:require [noir.validation :as vali]
            [noir.session :as session]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [nooz.db :as db])
  (:use korma.core)
  (:import java.sql.Timestamp
           java.net.URI
           java.net.URISyntaxException))

(defn get-comment-count-for-user [user]
  (let [user-id (:id user)]
    (:cnt (first (select db/comments
                   (aggregate (count :*) :cnt)
                   (where (= :user_id user-id)))))))

(defn get-comment-count-for-post [post]
  (let [post-id (:id post)]
    (:cnt (first (select db/comments
                   (aggregate (count :*) :cnt)
                   (where (= :post_id post-id)))))))
