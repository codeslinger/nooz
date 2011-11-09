(ns nooz.views.toplevel
  (:require [nooz.views.common :as common])
  (:use [noir.core :only [defpage]]
        [hiccup.core :only [html]]
        [hiccup.page-helpers :only [link-to]]
        [hiccup.form-helpers :only [form-to
                                    text-field
                                    password-field
                                    drop-down]]))

(defpage "/" []
  (common/layout
   "Top Headlines"
   ""))

(defpage "/latest" []
  (common/layout
   "Latest Headlines"
   ""))

(defpage "/about" []
  (common/layout
   "About this site"
   [:p "Nooz is a collaborative news site, a la Hacker News or Reddit, but with a twist. You can reply to a post either by commenting or submitting your reply via URL, e.g. from your own blog post. If you choose to reply via URL, the reply will show up immediately. If you choose to comment on this site, the comment will not be shown for a period of 24 hours after the original post was submitted. Comments are also limited to 1000 characters at most. Once a post has been submitted for 24 hours, all the comments on that post are shown and commenting is then closed. This promotes a more thoughtful dialogue and less inane commenting."]
   [:p
    "The source code for this application is available "
    (link-to "http://github.com/codeslinger/nooz" "on Github")
    ". Feel free to peruse, fork, issue pull requests, etc. Bug reports and pull requests are welcome!"]))

(defpage [:get "/submit"] []
  (common/layout
   "Submit an article"
   (form-to [:post "/submit"]
     [:fieldset
      [:div.clearfix
       [:label {:for "title"} "Title"]
       [:div.input (text-field {:class "span8"} "title")]]
      [:div.clearfix
       [:label {:for "url"} "Link"]
       [:div.input (text-field {:class "span8"} "url")]]
      [:div.clearfix
       [:label {:for "expiry"} "Comments close in"]
       [:div.input
        (drop-down {:class "small"} "expiry" [4 8 16 24])
        [:span.help-inline " hours"]]]
      [:div.actions [:button.primary.btn "Submit"]]])))

(defpage [:post "/submit"] {:keys [title url expiry]}
  (str "You submitted a story for " url " with the title " title " that expires in " expiry " hours"))
