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

(def *min-title-length* 3)
(def *max-title-length* 80)
(def *max-url-length* 1024)

(defn- valid-url? [url]
  (try
   (let [u (URI. url)]
     (or
      (= (.getScheme u) "http")
      (= (.getScheme u) "https")))
   (catch URISyntaxException e
     false)))

(defn- valid-new-post? [{:keys [title url] :as post}]
  (vali/rule (and (vali/has-value? title)
                  (vali/min-length? title *min-title-length*))
             [:title (str "Title must be at least " *min-title-length* " characters.")])
  (vali/rule (or (vali/errors? :title)
                 (vali/max-length? title *max-title-length*))
             [:title (str "Too long. " *max-title-length* " characters or less, please.")])
  (vali/rule (vali/has-value? url)
             [:url "URL cannot be blank."])
  (vali/rule (or (vali/errors? :url)
                 (vali/max-length? url *max-url-length*))
             [:url (str "Too long. " *max-url-length* " characters or less, please.")])
  (vali/rule (or (vali/errors? :url)
                 (valid-url? url))
             [:url "We only accept HTTP, HTTPS or FTP URLs."])
  (not (vali/errors? :title :url)))

(defn- insert-post! [post user time]
  (insert db/posts
    (values {:title (:title post)
             :url (:url post)
             :user_id (:id user)
             :created_at (Timestamp. time)})))

(defn create-post! [post user]
  (if (valid-new-post? post)
    (let [now (coerce/to-long (time/now))]
      (insert-post! post user now))))

(defn get-post-by-id [id]
  (first (select db/posts (where {:id id}) (limit 1))))

(defn get-latest-posts []
  (select db/posts (order :created_at :DESC) (limit 30)))

(defn get-post-count-for-user [user]
  (let [user-id (:id user)]
    (:cnt (first (select db/posts
                   (aggregate (count :*) :cnt)
                   (where (= :user_id user-id)))))))


