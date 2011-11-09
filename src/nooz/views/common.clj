(ns nooz.views.common
  (:use [noir.core :only [defpartial]]
        [hiccup.page-helpers :only [include-css html5 link-to]]
        [hiccup.form-helpers :only [form-to text-field password-field]]))

(defpartial layout [page-name & content]
  (html5
   [:head
    [:title "Nooz"]
    (include-css "http://twitter.github.com/bootstrap/1.4.0/bootstrap.min.css")
    (include-css "/css/style.css")]
   [:body
    [:div.topbar
     [:div.fill
      [:div.container
       (link-to {:class "brand"} "/" "Nooz")
       [:ul.nav
        [:li.active (link-to "/" "Top")]
        [:li (link-to "/latest" "Latest")]
        [:li (link-to "/submit" "Submit")]
        [:li (link-to "/about" "About")]]
       (form-to {:class "pull-right"} [:post "/login"]
        (text-field {:class "input-small" :placeholder "Username"} "username")
        (password-field {:class "input-small" :placeholder "Password"} "password")
        [:button.btn "Sign in"])]]]
    [:div.container
     [:div.content
      [:div.page-header
       [:h1 page-name]]
      [:div.row
       [:div.span14
        [:h2 content]]]]
     [:footer
      [:p "&copy; 2011 "
       (link-to "http://cbcg.net/" "Cipher Block Chain Gang")]]]]))
