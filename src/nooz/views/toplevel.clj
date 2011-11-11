(ns nooz.views.toplevel
  (:require [nooz.views.common :as common]
            [noir.response :as resp]
            [noir.session :as session])
  (:use [noir.core :only [defpage pre-route]]
        [hiccup.core :only [html]]
        [hiccup.page-helpers :only [link-to]]
        [hiccup.form-helpers :only [form-to
                                    label
                                    text-field
                                    password-field
                                    drop-down]]
        [nooz.models.user :as user]))

(pre-route "/submit" {}
           (when-not (session/get :username)
             (resp/redirect "/")))

(defpage "/" []
  (common/layout
   "Top Headlines"
   ""))

(defpage "/latest" []
  (common/layout
   "Latest Headlines"
   ""))

(defpage [:get "/submit"] []
  (common/layout
   "Submit a headline"
   (form-to [:post "/submit"]
     [:fieldset
      [:div.clearfix
       (label "title" "Title")
       [:div.input (text-field {:class "span8"} "title")]]
      [:div.clearfix
       (label "url" "Link")
       [:div.input (text-field {:class "span8"} "url")]]
      [:div.clearfix
       (label "expiry" "Comments close in")
       [:div.input
        (drop-down {:class "small"} "expiry" [24 12 8 4])
        [:span.help-inline "hours"]]]
      [:div.actions [:button.primary.btn "Submit"]]])))

(defpage [:post "/submit"] {:keys [title url expiry]}
  (str "You submitted a story for " url " with the title " title " that expires in " expiry " hours"))

(defpage "/about" []
  (common/layout
   "What is Nooz?"
   [:p "Nooz is a collaborative news site, a la Hacker News, but with a twist. You can reply to a post either by commenting or submitting your reply via URL, e.g. from your own blog post. If you choose to reply via URL, the reply will show up immediately. If you choose to comment on this site, the comment will not be shown for a period of time set by the poster after the original post was submitted. Once a post has existed for its configured period of time, all the comments on that post are shown and commenting is then closed. Comments are limited to 1000 characters and you can only comment once on any given post. You can, however, edit your comment up until the time commenting closes."]
   [:p
    "The source code for this application is available "
    (link-to "http://github.com/codeslinger/nooz" "on Github")
    ". Feel free to peruse, fork, issue pull requests, etc. Bug reports and pull requests are welcome!"]))
