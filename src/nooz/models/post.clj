(ns nooz.models.post
  (:require [noir.validation :as vali]
            [noir.session :as session]
            [redis.core :as redis]
            [nooz.crypto :as crypto]
            [nooz.time :as nt]
            [nooz.db :as db])
  (:import java.net.URI
           java.net.URISyntaxException))

(def min-title-length 3)
(def max-title-length 80)
(def max-url-length 1024)
(def posts-per-page 30)
(def latest-news-key "posts.time")
(def top-news-key "posts.top")
(def url-repost-barrier-seconds (* 3600 48))
(def user-repost-barrier-seconds (* 60 15))

(defn- url-key [url] (str "url:" url))
(defn- post-key [id] (str "post:" id))
(defn- user-posts-key [username] (str "user:" username ":posts"))
(defn- user-repost-key [username] (str "user:" username ":repost"))

(defn- valid-url? [url]
  (try
   (let [u (URI. url)]
     (or (= (.getScheme u) "http")
         (= (.getScheme u) "https")))
   (catch URISyntaxException e
     false)))

(defn- url-can-be-posted? [post]
  (redis/with-server db/prod-redis
    (nil? (redis/get (url-key (get post "url"))))))

(defn- user-can-post? [{:keys [username] :as user}]
  (redis/with-server db/prod-redis
    (nil? (redis/get (user-repost-key username)))))

(defn- valid-new-post? [{:keys [title url] :as post} user]
  (vali/rule (user-can-post? user)
             [:title "You're posting too fast. Slow down, chief."])
  (vali/rule (and (vali/has-value? title)
                  (vali/min-length? title min-title-length))
             [:title (str "Title must be at least " min-title-length " characters.")])
  (vali/rule (or (vali/errors? :title)
                 (vali/max-length? title max-title-length))
             [:title (str "Too long. " max-title-length " characters or less, please.")])
  (vali/rule (vali/has-value? url)
             [:url "URL cannot be blank."])
  (vali/rule (or (vali/errors? :url)
                 (vali/max-length? url max-url-length))
             [:url (str "Too long. " max-url-length " characters or less, please.")])
  (vali/rule (or (vali/errors? :url)
                 (valid-url? url))
             [:url "We only accept HTTP or HTTPS URLs."])
  (vali/rule (or (vali/errors? :url)
                 (url-can-be-posted? url))
             [:url "This URL has already been posted."])
  (not (vali/errors? :title :url)))

(defn- record-post! [post]
  (let [args (flatten (cons (post-key (get post "id")) (seq post)))]
    (apply redis/hmset args)))

(defn- record-post-for-user! [post user time]
  (let [username (:username user)
        post-id (get post "id")]
    (redis/zadd (user-posts-key username) time post-id)))

(defn- add-news-chronologically! [post time]
  (let [post-id (get post "id")]
    (redis/zadd latest-news-key time post-id)))

(defn- add-news-by-rank! [post item]
  (let [post-id (get post "id")
        score (get post "score")]
    (redis/zadd top-news-key score post-id)))

(defn- create-url-barrier! [post]
  (redis/setex (url-key (get post "url"))
               url-repost-barrier-seconds
               (get post "id")))

(defn- create-repost-barrier! [user]
  (redis/setex (user-repost-key (:username user))
               user-repost-barrier-seconds
               "1"))

(defn- create-post-record [post user time]
  (-> {}
      (assoc "id" (crypto/gen-id))
      (assoc "title" (:title post))
      (assoc "url" (:url post))
      (assoc "username" (:username user))
      (assoc "created_at" time)
      (assoc "score" 0)
      (assoc "comments" 0)))

(defn- save-post! [p user time]
  (let [post (create-post-record p user time)]
    (redis/with-server db/prod-redis
      (record-post! post)
      (record-post-for-user! post user time)
      (add-news-chronologically! post time)
      (add-news-by-rank! post time)
      (create-url-barrier! post)
      (create-repost-barrier! user))))

(defn create-post! [post user]
  (if (valid-new-post? post user)
    (let [now (nt/long-now)]
      (save-post! post user now))))

(defn get-post [id]
  (redis/with-server db/prod-redis
    (redis/hgetall (post-key id))))

(defn get-latest-posts [& start]
  (let [index (or start 0)]
    (redis/with-server db/prod-redis
      (redis/zrange latest-news-key
                    index
                    (+ index (- posts-per-page 1))))))

(defn get-ranked-posts [& start]
  (let [index (or start 0)]
    (redis/with-server db/prod-redis
      (redis/zrange top-news-key
                    index
                    (+ index (- posts-per-page 1))))))

(defn get-post-count-for-user [user]
  (redis/with-server db/prod-redis
    (redis/zcard (user-posts-key (:username user)))))

(defn get-posts-for-user [username & start]
  (let [index (or start 0)]
    (redis/with-server db/prod-redis
      (redis/zrange (user-posts-key username)
                    index
                    (+ index (- posts-per-page 1))))))
