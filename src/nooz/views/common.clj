(ns nooz.views.common
  (:require [noir.session :as session])
  (:use [noir.core :only [defpartial]]
        [hiccup.page-helpers :only [include-css html5 link-to]]
        [hiccup.form-helpers :only [form-to text-field password-field]]
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
            [:li (link-to "/submit" "Submit")])
          [:li (link-to "/about" "About")]]
         [:span.pull-right
          (if (nil? username)
            [:ul.nav
             [:li (link-to "/register" "Sign Up")]
             [:li (link-to "/login" "Login")]]
            [:ul.nav
             [:li (link-to "/profile" username)]
             [:li (link-to "/logout" "logout")]])]]]]
      [:div.container
       [:div.content body]
       [:footer
        [:p "&copy; 2011 "
         (link-to "http://cbcg.net/" "Cipher Block Chain Gang")]]]])))

(defpartial layout [page-name & content]
  (page-wrapper
   [:div.page-header [:h1 page-name]]
   [:div.row
    [:div.span14
     (session/flash-get)
     [:h2 content]]]))

