(ns nooz.views.toplevel
  (:require [nooz.views.common :as common]
            [noir.response :as resp]
            [noir.session :as session]
            [noir.validation :as vali]
            [clojure.string :as string]
            [clj-time.core :as time]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [nooz.models.user :as user]
            [nooz.models.post :as post]
            [nooz.models.comment :as comment])
  (:use [noir.core :only [defpage pre-route defpartial render]]
        [hiccup.core :only [html
                            h]]
        [hiccup.page-helpers :only [link-to]]
        [hiccup.form-helpers :only [form-to
                                    label
                                    text-field
                                    password-field
                                    drop-down]]
        [nooz.models.user :as user])
  (:import java.net.URI))

(defpartial new-post-form [{:keys [title url expiry] :as post}]
  (form-to [:post "/submit"]
    [:fieldset
     (common/form-item :title "Title" (text-field {:class "span6"} "title" title))
     (common/form-item :url "URL" (text-field {:class "span6"} "url" url))
     [:div.actions [:button.primary.btn "Submit"]]]))

(defn long-date [t]
  (tc/to-long (tc/from-date t)))

(defn get-host [url]
  (.getHost (URI. url)))

(defpartial post-list-item [{:keys [id
                                    title
                                    url
                                    user_id
                                    created_at] :as post}
                            now]
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
      [:span (str (common/time-ago-in-words
                   (long-date created_at)
                   now) " ago")]
      [:span " "]
      (let [comments (comment/get-comment-count-for-post post)
            label (if (= 1 comments) "comment" "comments")]
        [:span "(" (link-to (str "/item/" id) (str comments " " label)) ")"])]]))

(defpartial post-list [time posts]
  (map #(post-list-item %1 time) posts))

(defpartial post-details [time post]
  (post-list-item post time))

(pre-route "/submit" {}
           (when-not (user/get-user-from-session)
             (common/borked "You must be logged in to make a submission.")))

(defpage "/" {}
  (common/layout "Top Submissions" ""))

(defpage "/latest" {}
  (common/layout
   "Latest Submissions"
   (post-list (tc/to-long (time/now))
              (post/get-latest-posts))))

(defpage "/item/:id" {:keys [id]}
  (let [post (post/get-post-by-id (Integer. id))]
    (if post
      (let [now (tc/to-long (time/now))]
        (common/layout "Submission Details" (post-details now post)))
      (common/borked "Sorry, we could not find the requested item."))))

(defpage "/submit" {:as post}
  (common/layout "New submission" (new-post-form post)))

(defpage [:post "/submit"] {:as post}
  (let [post (post/create-post! post (user/get-user-from-session))]
    (if-not (nil? post)
      (resp/redirect (str "/item/" (:id post)))
      (do
        (common/boo! "There were some problems with your submission.")
        (render "/submit" post)))))

