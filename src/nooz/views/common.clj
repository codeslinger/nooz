(ns nooz.views.common
  (:use [noir.core :only [defpartial]]
        [hiccup.page-helpers :only [include-css html5 link-to]]
        [hiccup.form-helpers :only [form-to text-field password-field]]))

(defpartial page-wrapper [& body]
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
        [:li (link-to "/" "Top")]
        [:li (link-to "/latest" "Latest")]
        [:li (link-to "/submit" "Submit")]
        [:li (link-to "/about" "About")]]
       [:span.pull-right
        [:ul.nav
         [:li (link-to "/register" "Sign Up")]
         [:li (link-to "/login" "Login")]]]]]]
    [:div.container
     [:div.content body]
     [:footer
      [:p "&copy; 2011 "
       (link-to "http://cbcg.net/" "Cipher Block Chain Gang")]]]]))

(defpartial layout [page-name & content]
  (page-wrapper
   [:div.page-header [:h1 page-name]]
   [:div.row [:div.span14 [:h2 content]]]))

(defpartial two-col-page [page-name sidebar & content]
  (page-wrapper
   [:div.page-header
    [:h1 page-name]]
   [:div.row
    [:div.span10 [:h2 content]]
    [:div.span4 [:h3 sidebar]]]))
