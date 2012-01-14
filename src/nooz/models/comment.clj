(ns nooz.models.comment
  (:require [noir.validation :as vali]
            [noir.session :as session]
            [redis.core :as redis]
            [nooz.constants :as const]
            [nooz.time :as nt]
            [nooz.db :as db]
            [nooz.models.post :as post]))

(defn- valid-comment? [{:keys [comment] :as args}]
  (vali/rule (and (vali/has-value? comment)
                  (vali/max-length? comment const/max-comment-length))
             [:comment (str "Comment must be between 1 - "
                            const/max-comment-length
                            " characters.")])
  (not (vali/errors? :comment)))

(defn- record-comment! [post comment]
  (let [key (const/post-comments-key (get post "id"))]
    (redis/hset key (get comment "id") (to-json comment))))

(defn- record-comment-for-user! [comment post_id user time]
  (let [username (:username user)
        comment-id (get comment "id")]
    (redis/zadd (const/user-comments-key username) (str post_id "|" comment_id))))

(defn- update-comment-count! [id]
  (redis/hincrby (post/post-key id) "comments" 1))

(defn- create-comment-record [user parent_id content time]
  (-> {}
      (assoc "id" (crypto/gen-id))
      (assoc "content" content)
      (assoc "username" (:username user))
      (assoc "created_at" time)
      (assoc "parent_id" parent_id)
      (assoc "score" 0)
      (assoc "comments" 0)))

(defn- save-comment! [user post time parent comment]
  (let [comment (create-comment-record user parent comment time)]
    (redis/with-server db/prod-redis
      (record-comment! comment)
      (update-comment-count! (get post "id"))
      (record-comment-for-user! comment user time))))

(defn create-comment! [post user {:keys [parent comment] :as args}]
  (if (valid-comment? args)
    (let [now nt/long-now]
      (save-comment! user post now parent comment))))

(defn get-comment-count-for-user [user] 0)

(defn get-comment-count-for-post [post] 0)
