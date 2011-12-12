(ns nooz.views.common
  (:require [noir.session :as session]
            [noir.validation :as vali]
            [noir.request :as req]
            [noir.response :as resp]
            [clojure.string :as string]
            [nooz.time :as nt])
  (:use [noir.core :only [defpartial]]
        [hiccup.core :only [h]]
        [hiccup.page-helpers :only [include-css
                                    html5
                                    link-to]]
        [hiccup.form-helpers :only [form-to
                                    text-field
                                    password-field
                                    label]]
        [nooz.server :only [*app-name*]]))

(defn nav-link [uri target label]
  (if (= uri target)
    [:li.active (link-to target label)]
    [:li (link-to target label)]))

;; ----- PARTIALS ------------------------------------------------------------

(defpartial page-wrapper [& body]
  (let [username (session/get :username)
        uri (:uri (req/ring-request))]
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
          (nav-link uri "/" "Top")
          (nav-link uri "/latest" "Latest")
          (if (not (nil? username))
            (nav-link uri "/submit" "Post"))]
         [:span.pull-right
          (if (nil? username)
            [:ul.nav
             (nav-link uri "/register" "Sign Up")
             (nav-link uri "/login" "Login")]
            [:ul.nav
             (nav-link uri (str "/user/" username) (h username))
             [:li (link-to "/logout" "logout")]])]]]]
      [:div.container
       [:div.content body]
       [:footer
        [:p
         [:span "&copy; 2011 " (link-to "http://cbcg.net/" "cipher block chain gang")]
         [:span " | "]
         [:span (link-to "http://github.com/codeslinger/nooz" "source")]]]]])))

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

;; ----- HELPER FUNCTIONS ----------------------------------------------------

(defn boo! [msg]
  (session/flash-put! (error-text [msg])))

(defn yay! [msg]
  (session/flash-put! (success-text [msg])))

(defn borked [msg]
  (boo! msg)
  (resp/redirect "/"))

;; ----- LAYOUTS -------------------------------------------------------------

(defpartial layout [& content]
  (page-wrapper
   [:div.row [:div.span14
              (session/flash-get)
              content]]))

(defpartial titled-layout [title & content]
  (page-wrapper
    [:div.page-header [:h1 title]]
    [:div.row [:div.span14
               (session/flash-get)
               content]]))
