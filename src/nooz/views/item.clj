(ns nooz.views.item
  (:require [noir.response :as resp]
            [noir.validation :as vali]
            [nooz.time :as nt]
            [nooz.views.common :as common]
            [nooz.models.user :as user]
            [nooz.models.post :as post]
            [nooz.models.comment :as comment])
  (:use [noir.core :only [defpage
                          pre-route
                          defpartial
                          render]]
        [hiccup.core :only [html
                            h]]
        [hiccup.page-helpers :only [link-to]]
        [hiccup.form-helpers :only [form-to
                                    label
                                    text-field
                                    text-area
                                    password-field
                                    drop-down]])
  (:import java.net.URI))

;; ----- HELPER FUNCTIONS ----------------------------------------------------

(defn get-host [url]
  (.getHost (URI. url)))

;; ----- PARTIALS ------------------------------------------------------------

(defpartial new-post-form [{:keys [title url expiry] :as post}]
  (form-to [:post "/submit"]
    [:fieldset
     (common/form-item :title "Title" (text-field {:class "span6"} "title" title))
     (common/form-item :url "URL" (text-field {:class "span6"} "url" url))
     [:div.actions [:button.primary.btn "Submit"]]]))

(defpartial post-list-item [{:keys [id title url user_id created_at] :as post} now]
  (let [username (user/get-name-for-id user_id)]
    [:div.clearfix.post
     [:div.span12
      (link-to {:class "title"} url (h title))
      [:span.dom (str " (" (get-host url) ")")]]
     [:div.subtext
      [:span "posted by"]
      [:span " "]
      [:span (link-to (str "/user/" username) (h username))]
      [:span " "]
      [:span (str (nt/time-ago-in-words (nt/long-date created_at) now) " ago")]
      [:span " "]
      (let [comments (comment/get-comment-count-for-post post)
            label (if (= 1 comments) "comment" "comments")]
        [:span "(" (link-to (str "/item/" id) (str comments " " label)) ")"])]]))

(defpartial post-list [time posts]
  (map #(post-list-item %1 time) posts))

(defpartial comment-form [post_id]
  (form-to [:post (str "/item/" post_id "/comments")]
    [:div.comment-box
     (text-area "comment")
     (vali/on-error :comment common/error-inline)
     [:span.help-block "1000 character maximum."]
     [:button.btn.small "submit comment"]]))

(defpartial post-details [time post]
  (post-list-item post time)
  (comment-form (:id post)))

;; ----- FILTERS -------------------------------------------------------------

(pre-route "/submit" {}
           (when-not (user/get-user-from-session)
             (common/borked "You must be logged in to make a submission.")))

;; ----- HTTP HANDLERS -------------------------------------------------------

(defpage "/item/:id" {:keys [id]}
  (let [post (post/get-post-by-id (Integer. id))]
    (if post
      (let [now (nt/as-long (nt/now))]
        (common/layout (post-details now post)))
      (common/borked "Sorry, we could not find the requested item."))))

(defpage [:post "/item/:id/comments"] {:as args}
  (let [post (post/get-post-by-id (Integer. (:id args)))]
    (if post
      (let [comment-id (comment/submit-comment! post args)]
        (if comment-id
          (resp/redirect (str "/item/" (:id post) "/comments/" comment-id))
          (do
            (common/boo! "There was a problem submitting your comment.")
            (resp/redirect (str "/item/" (:id post))))))
      (common/borked "Sorry, we could not find the requested item."))))

(defpage "/submit" {:as post}
  (common/titled-layout "New submission" (new-post-form post)))

(defpage [:post "/submit"] {:as post}
  (let [post (post/create-post! post (user/get-user-from-session))]
    (if post
      (resp/redirect (str "/item/" (:id post)))
      (do
        (common/boo! "There were some problems with your submission.")
        (render "/submit" post)))))
