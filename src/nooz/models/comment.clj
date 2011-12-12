(ns nooz.models.comment
  (:require [noir.validation :as vali]
            [noir.session :as session]
            [nooz.time :as nt]
            [nooz.db :as db]))

(def *max-comment-length* 1000)

(defn get-comment-count-for-user [user] 0)

(defn get-comment-count-for-post [post] 0)

(defn- valid-comment? [{:keys [comment] :as args}]
  (vali/rule (and (vali/has-value? comment)
                  (vali/max-length? comment *max-comment-length*))
             [:comment (str "Comment must be between 1 - "
                            *max-comment-length*
                            " characters.")])
  (not (vali/errors? :comment)))

(defn submit-comment! [post args] nil)
