(ns nooz.views.auth
  (:use [noir.core :only [defpage]]))

(defpage [:post "/login"] {:keys [username password]}
  (str "You attempted to log in as " username " with the password " password))

(defpage "/logout" []
  (str "You have been logged out"))

(defpage "/profile" []
  (str "You have requested your profile"))
