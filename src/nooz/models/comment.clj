(ns nooz.models.comment
  (:require [noir.validation :as vali]
            [noir.session :as session]
            [nooz.time :as nt]
            [nooz.db :as db]
            [nooz.models.user :as user])
  (:use korma.core))

(def *max-comment-length* 1000)

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

(defn- valid-comment? [{:keys [comment] :as args}]
  (vali/rule (and (vali/has-value? comment)
                  (vali/max-length? comment *max-comment-length*))
             [:comment (str "Comment must be between 1 - "
                            *max-comment-length*
                            " characters.")])
  (not (vali/errors? :comment)))

(defn- insert-comment! [comment user time]
  (insert db/comments
    (values {:comment (:comment comment)
             :post_id (Integer. (:post_id comment))
             :user_id (:id user)
             :created_at (nt/to-sql time)})))

(defn submit-comment! [post args]
  (if (valid-comment? args)
    (let [now (nt/as-long (nt/now))]
      (insert-comment! args post (user/get-user-from-session) now))))
