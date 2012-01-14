(ns nooz.constants)

;; app-related constants
(def app-host "localhost:8080")
(def app-name "Nooz")

;; session-related constants
(def secret-key "DEADBEEFCAFEMOOP")
(def max-session-age-seconds 1209600)

;; constants for validation of various items
(def min-username-length 3)
(def max-username-length 64)
(def max-about-length 256)
(def max-email-length 256)
(def max-comment-length 1000)
(def min-title-length 3)
(def max-title-length 80)
(def max-url-length 1024)
(def posts-per-page 30)
(def url-repost-barrier-seconds (* 60 60 48))
(def user-repost-barrier-seconds (* 60 15))

;; keys used for database storage and retrieval
(def latest-news-key "posts.time")
(def top-news-key "posts.top")
(defn url-key [url] (str "url:" url))
(defn post-key [id] (str "post:" id))
(defn user-posts-key [username] (str "user:" username ":posts"))
(defn user-repost-key [username] (str "user:" username ":repost"))
(defn user-key [username] (str "user:" username))
(defn email-key [email] (str "email:" email))
(defn token-key [token] (str "token:" token))
(defn comment-key [id] (str "comment:" id))
(defn post-comments-key [id] (str "post.cmts:" id))
(defn user-comments-key [username] (str "user.cmts:" username))
