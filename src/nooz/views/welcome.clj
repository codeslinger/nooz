(ns nooz.views.welcome
  (:require [nooz.views.common :as common])
  (:use [noir.core :only [defpage]]
        [hiccup.core :only [html]]))

(defpage "/" []
  (common/layout "Best stuff"))

(defpage "/latest" []
  (common/layout "Latest stuff"))

(defpage "/about" []
  (common/layout "About this site"))

(defpage [:get "/submit"] []
  (common/layout "Submit a new story"))

(defpage [:post "/login"] {:keys [username password]}
  (str "You attempted to log in as " username " with the password " password))

(defpage [:post "/submit"] {:keys [title url]}
  (str "You submitted a story for " url " with the title " title))
