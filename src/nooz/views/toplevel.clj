(ns nooz.views.toplevel
  (:require [nooz.views.common :as common]
            [noir.response :as resp]
            [noir.session :as session]
            [noir.validation :as vali]
            [clojure.string :as string]
            [clojure.contrib.math :as math]
            [clj-time.core :as time]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [nooz.models.user :as user]
            [nooz.models.post :as post])
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

(defn time-ago-in-words [from to]
  (let [diff (/ (- to from) 1000 60)]
    (cond
      (< diff 1) "less than a minute"
      (< diff 44) (str (math/round diff) " minutes")
      (< diff 89) "about an hour"
      (< diff 1439) (str "about " (math/round (/ diff 60.0)) " hours")
      (< diff 2519) "about a day"
      (< diff 43199) (str "about " (math/round (/ diff 1440.0)) " days")
      (< diff 86399) "about a month"
      (< diff 525599) (str "about " (math/round (/ diff 43200.0)) " months")
      :else "many months")))

(defpartial new-post-form [{:keys [title url expiry] :as post}]
  (form-to [:post "/submit"]
    [:fieldset
     (common/form-item :title "Title" (text-field {:class "span6"} "title" title))
     (common/form-item :url "URL" (text-field {:class "span6"} "url" url))
     (common/form-item :expiry "Comments close in"
                       (drop-down {:class "small"}
                                  "expiry"
                                  post/*valid-expiry-times*
                                  (Integer. (or expiry 0))))
     [:div.actions [:button.primary.btn "Submit"]]]))

(defn long-date [t]
  (tc/to-long (tc/from-date t)))

(defn get-host [url]
  (.getHost (URI. url)))

(defpartial post-list-item [{:keys [id title url expiry user_id created_at] :as post} now]
  [:div.clearfix.post
   [:div.span12
    (link-to {:class "title"} url (h title))
    [:span.dom (str " (" (get-host url) ")")]]
   [:div.subtext
    [:span "posted by " (link-to (str "/user/" user_id) (h (user/get-name-for-id user_id)))]
    [:span " "]
    [:span (str (time-ago-in-words (long-date created_at) now) " ago")]
    [:span " "]
    [:span "(" (link-to (str "/post/" id) "0 replies") ")"]]])

(defpartial post-list [time posts]
  (map #(post-list-item %1 time) posts))

(pre-route "/submit" {}
           (when-not (user/get-user-from-session)
             (common/borked "You must be logged in to submit a headline.")))

(defpage "/" {}
  (common/layout "Top Headlines" ""))

(defpage "/latest" {}
  (common/layout
   "Latest Headlines"
   (post-list (tc/to-long (time/now)) (post/get-latest-posts))))

(defpage "/item/:id" {:keys [id]}
  (let [post (post/get-post-by-id (Integer. id))]
    (if post
      (common/layout "Headline Details" "")
      (common/borked "Sorry, we could not find the requested item."))))

(defpage [:get "/submit"] {:as post}
  (common/layout "Submit a headline" (new-post-form post)))

(defpage [:post "/submit"] {:as post}
  (let [post (post/create-post! post (user/get-user-from-session))]
    (if-not (nil? post)
      (resp/redirect (str "/item/" (:id post)))
      (do
        (common/boo! "There were some problems with your submission.")
        (render "/submit" post)))))

(defpage "/about" []
  (common/layout
   "What is Nooz?"
   [:p "Nooz is a collaborative news site, a la Hacker News, but with a
twist. You can reply to a post either by commenting or submitting your
reply via URL, e.g. from your own blog post. If you choose to reply via
URL, the reply will show up immediately. If you choose to comment on this
site, the comment will not be shown for a period of time set by the
poster after the original post was submitted. Once a post has existed
for its configured period of time, all the comments on that post are
shown and commenting is then closed. Comments are limited to 1000
characters and you can only comment once on any given post. You can
however, edit your comment up until the time commenting closes."]
   [:p "The source code for this application is available "
    (link-to "http://github.com/codeslinger/nooz" "on Github")
    ". Feel free to peruse, fork, issue pull requests, etc. Bug
reports and pull requests are welcome!"]))
