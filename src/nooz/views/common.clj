(ns nooz.views.common
  (:require [noir.session :as session]
            [noir.validation :as vali]
            [clojure.string :as string]
            [clojure.contrib.math :as math]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]
            [noir.response :as resp])
  (:use [noir.core :only [defpartial]]
        [hiccup.core :only [h]]
        [hiccup.page-helpers :only [include-css html5 link-to]]
        [hiccup.form-helpers :only [form-to text-field password-field label]]
        [nooz.models.user :as user]
        [nooz.server :only [*app-name*]]))

(defpartial page-wrapper [& body]
  (let [username (session/get :username)]
    (html5
     [:head
      [:title *app-name*]
      (include-css "http://twitter.github.com/bootstrap/1.4.0/bootstrap.min.css")
      (include-css "/css/style.css")]
     [:body
      [:div.topbar
       [:div.fill
        [:div.container
         (link-to {:class "brand"} "/" *app-name*)
         [:ul.nav
          [:li (link-to "/" "Top")]
          [:li (link-to "/latest" "Latest")]
          (if (not (nil? username))
            [:li (link-to "/submit" "Submit")])]
         [:span.pull-right
          (if (nil? username)
            [:ul.nav
             [:li (link-to "/register" "Sign Up")]
             [:li (link-to "/login" "Login")]]
            [:ul.nav
             [:li (link-to (str "/user/" username) (h username))]
             [:li (link-to "/logout" "logout")]])]]]]
      [:div.container
       [:div.content body]
       [:footer
        [:p
         [:span "&copy; 2011 " (link-to "http://cbcg.net/" "cipher block chain gang")]
         [:span " | "]
         [:span (link-to "http://github.com/codeslinger/nooz" "source")]]]]])))

(defpartial layout [page-name & content]
  (page-wrapper
   [:div.page-header [:h1 page-name]]
   [:div.row
    [:div.span14
     (session/flash-get)
     content]]))

(defpartial error-text [errors]
  [:p.alert-message.error (string/join "<br/>" errors)])

(defpartial success-text [messages]
  [:p.alert-message.success (string/join "<br/>" messages)])

(defpartial error-inline [errors]
  (if (not (nil? errors))
    [:span.help-inline (string/join "<br/>" errors)]))

(defpartial form-item [key title & value]
  [(if (vali/errors? key) :div.clearfix.error :div.clearfix)
   (label (name key) title)
   [:div.input value (vali/on-error key error-inline)]])

(defn boo! [msg]
  (session/flash-put! (error-text [msg])))

(defn yay! [msg]
  (session/flash-put! (success-text [msg])))

(defn borked [msg]
  (boo! msg)
  (resp/redirect "/"))

(defn time-ago-in-words [from to]
  (let [diff (/ (- to from) 1000 60)]
    (cond
      (< diff 1) "less than a minute"
      (< diff 44) (str (math/round diff) " minutes")
      (< diff 89) "one hour"
      (< diff 1439) (str (math/round (/ diff 60.0)) " hours")
      (< diff 2519) "one day"
      (< diff 43199) (str (math/round (/ diff 1440.0)) " days")
      (< diff 86399) "one month"
      (< diff 525599) (str (math/round (/ diff 43200.0)) " months")
      :else "many months")))

(defn human-time [date]
  (time-ago-in-words (tc/to-long (tc/from-date date))
                     (tc/to-long (time/now))))
