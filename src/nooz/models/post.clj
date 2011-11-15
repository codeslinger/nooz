(ns nooz.models.post
  (:require [noir.validation :as vali]
            [noir.session :as session]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [nooz.crypto :as crypto]
            [nooz.db :as db]
            [nooz.mail :as mail])
  (:use korma.core)
  (:import java.sql.Timestamp
           java.net.URI
           java.net.URISyntaxException))

(def *valid-expiry-times* [24 16 8 4])
(def *millis-per-hour* (* 60 60 1000))

(defn- make-expiry [now hours]
  (Timestamp. (+ now (* (Long. hours) *millis-per-hour*))))

(defn- valid-url? [url]
  (try
   (let [u (URI. url)]
     (or
      (= (.getScheme u) "http")
      (= (.getScheme u) "https")))
   (catch URISyntaxException e
     false)))

(defn in?
  "true if coll contains elem"
  [coll elem]
  (some #{elem} coll))

(defn- valid-new-post? [{:keys [title url expiry] :as post}]
  (vali/rule (and (vali/has-value? title)
                  (vali/min-length? title 3))
             [:title "Title must be at least 3 characters."])
  (vali/rule (vali/has-value? url)
             [:url "URL cannot be blank."])
  (vali/rule (or (vali/errors? :url)
                 (valid-url? url))
             [:url "Malformed URL."])
  (vali/rule (vali/has-value? expiry)
             [:expiry "Expiry cannot be empty."])
  (vali/rule (in? *valid-expiry-times* (Integer. expiry))
             [:expiry "Invalid expiration length."])
  (not (vali/errors? :title :url :expiry)))

(defn- insert-post! [post user time]
  (insert db/posts
    (values {:title (:title post)
             :url (:url post)
             :user_id (:id user)
             :expires_at (make-expiry time (:expiry post))
             :created_at (Timestamp. time)})))

(defn create-post! [post user]
  (if (valid-new-post? post)
    (let [now (coerce/to-long (time/now))]
      (insert-post! post user now))))

(defn get-post-by-id [id]
  (select db/posts (where {:id id}) (limit 1)))
