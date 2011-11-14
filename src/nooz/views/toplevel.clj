(ns nooz.views.toplevel
  (:require [nooz.views.common :as common]
            [noir.response :as resp]
            [noir.session :as session]
            [noir.validation :as vali]
            [clojure.string :as string]
            [nooz.models.user :as user]
            [nooz.models.post :as post])
  (:use [noir.core :only [defpage pre-route defpartial render]]
        [hiccup.core :only [html]]
        [hiccup.page-helpers :only [link-to]]
        [hiccup.form-helpers :only [form-to
                                    label
                                    text-field
                                    password-field
                                    drop-down]]
        [nooz.models.user :as user]))

(defpartial new-post-form [{:keys [title url expiry] :as post}]
  (form-to [:post "/submit"]
    [:fieldset
     (common/form-item :title "Title"
                       (text-field {:class "span6"} "title" title))
     (common/form-item :url "URL"
                       (text-field {:class "span6"} "url" url))
     (common/form-item :expiry "Comments close in"
                       (drop-down {:class "small"} "expiry"
                                  post/*valid-expiry-times*
                                  (Integer. (or expiry 0))))
     [:div.actions [:button.primary.btn "Submit"]]]))

(pre-route "/submit" {}
           (when-not (user/get-user-from-session)
             (do
               (session/flash-put!
                (common/error-text ["You must be logged in to submit a headline."]))
               (resp/redirect "/"))))

(defpage "/" []
  (common/layout "Top Headlines" ""))

(defpage "/latest" []
  (common/layout "Latest Headlines" ""))

(defpage [:get "/submit"] {:as post}
  (common/layout "Submit a headline" (new-post-form post)))

(defpage [:post "/submit"] {:as post}
  (if (post/create-post! post (user/get-user-from-session))
    (resp/redirect "/latest")
    (do
      (session/flash-put!
       (common/error-text ["There were some problems with your submission."]))
      (render "/submit" post))))

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
