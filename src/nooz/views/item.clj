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

(defpartial post-list-item [post now]
  (let [id (get post "id")
        title (get post "title")
        url (get post "url")
        username (get post "username")
        created_at (Long. (get post "created_at"))]
    [:div.clearfix.post
     [:div.span12
      (link-to {:class "title"} url (h title))
      [:span.dom (str " (" (get-host url) ")")]]
     [:div.subtext
      [:span "posted by"]
      [:span " "]
      [:span (link-to (str "/user/" username) (h username))]
      [:span " "]
      [:span (str (nt/time-ago-in-words created_at now) " ago")]
      [:span " "]
      (let [comments (comment/get-comment-count-for-post post)
            label (if (= 1 comments) "comment" "comments")]
        [:span "(" (link-to (str "/item/" id) (str comments " " label)) ")"])]]))

(defpartial post-list [time posts]
  (let [full-posts (map #(post/get-post %1) posts)]
    (map #(post-list-item %1 time) full-posts)))

(defpartial comment-form [post_id]
  (form-to [:post (str "/item/" post_id "/comments")]
    [:div.comment-box
     (text-area "comment")
     (vali/on-error :comment common/error-inline)
     [:span.help-block "1000 character maximum."]
     [:button.btn.small "submit comment"]]))

(defpartial post-details [time post]
  (post-list-item post time)
  (comment-form (get post "id")))

;; ----- FILTERS -------------------------------------------------------------

(pre-route "/submit" {}
           (when (empty? (user/logged-in-user))
             (common/borked "You must be logged in to make a submission.")))

;; ----- HTTP HANDLERS -------------------------------------------------------

(defpage "/item/:id" {:keys [id]}
  (let [post (post/get-post id)]
    (if (or (nil? post)
            (empty? post))
      (common/borked "Sorry, we could not find the requested item.")
      (let [now (nt/long-now)]
        (common/layout (post-details now post))))))

(defpage [:post "/item/:id/comments"] {:as args}
  (let [post (post/get-post (:id args))]
    (if post
      (let [comment-id (comment/submit-comment! post args)]
        (if comment-id
          (resp/redirect (str "/item/" (:id post) "/comments/" comment-id))
          (do
            (common/boo! "There was a problem submitting your comment.")
            (resp/redirect (str "/item/" (:id post))))))
      (common/borked "Sorry, we could not find the requested item."))))

(defpage "/submit" {:as post}
  (common/titled-layout "New post" (new-post-form post)))

(defpage [:post "/submit"] {:as post}
  (let [post (post/create-post! post (user/logged-in-user))]
    (if post
      (resp/redirect (str "/item/" (get post "id")))
      (do
        (common/boo! "There were some problems with your submission.")
        (render "/submit" post)))))
