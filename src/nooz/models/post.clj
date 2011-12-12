(ns nooz.models.post
  (:require [noir.validation :as vali]
            [noir.session :as session]
            [redis.core :as redis]
            [nooz.crypto :as crypto]
            [nooz.mail :as mail]
            [nooz.time :as nt]
            [nooz.db :as db])
  (:import java.net.URI
           java.net.URISyntaxException))

(def *min-title-length* 3)
(def *max-title-length* 80)
(def *max-url-length* 1024)

(defn- post-key [post] (str "post:" (:id post)))
(defn- user-posts-key [username] (str "p4u:" username))

(defn- valid-url? [url]
  (try
   (let [u (URI. url)]
     (or (= (.getScheme u) "http")
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

(defn- record-user-post! [post user time]
  (let [username (:username user)
        post-id (:id post)]
    (redis/zadd (user-posts-key username) (str time ":" post-id))))

(defn- create-post-record [post user time]
  (-> {}
      (assoc :id (crypto/gen-id))
      (assoc :title (:title post))
      (assoc :url (:url post))
      (assoc :username (:username user))
      (assoc :created_at time)
      (assoc :score 0)))

(defn- save-post! [post user time]
  (let [final (create-post-record post user time)]
    (redis/with-server db/prod-redis
      (redis/atomically
       (redis/hmset (post-key post) (seq final))
       (record-user-post! post user time)))))

(defn create-post! [post user]
  (if (valid-new-post? post)
    (let [now (nt/long-now)]
      (save-post! post user now))))

(defn get-post [id]
  (redis/with-server db/prod-redis
    (redis/hgetall (post-key id))))

(defn get-latest-posts [] nil)

(defn get-post-count-for-user [user]
  (redis/with-server db/prod-redis
    (redis/zcard (user-posts-key (:username user)))))

(defn get-posts-for-user [username & start]
  (let [index (or start 0)]
    (redis/with-server db/prod-redis
      (redis/zrange (user-posts-key username)) index (+ index 30))))



