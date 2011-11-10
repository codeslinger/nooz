(ns nooz.views.auth
  (:require [nooz.views.common :as common])
  (:use [noir.core :only [defpage]]
        [hiccup.form-helpers :only [form-to
                                    label
                                    text-field
                                    password-field
                                    drop-down]])
  (:import org.mindrot.jbcrypt.BCrypt))

;;
;; Private functions
;;

(defn- gen-salt
  "Generate a salt for BCrypt's hashing routines."
  ([]
     (BCrypt/gensalt))
  ([rounds]
     (BCrypt/gensalt rounds)))

(defn- gen-hash
  "Hash the given string with a generated or supplied salt. Uses BCrypt hashing algorithm. The given salt (if supplied) needs to be in a format acceptable by BCrypt. Its recommended one use `gen-salt` for generating salts."
  ([raw]
     (gen-hash (gen-salt) raw))
  ([salt raw]
     (BCrypt/hashpw raw salt)))

(defn- compare-hashes
  "Compare a raw string with an already hashed string"
  [raw hashed]
  (BCrypt/checkpw raw hashed))

;;
;; Page controllers
;;

(defpage "/login" []
  (common/two-col-page
   "Login"
   [:div.span4
    [:h3 "Don't have a login?"]
    [:button.success.btn "Sign up!"]]
   [:div.span10
    (form-to [:post "/login"]
      [:fieldset
       [:div.clearfix
        (label "username" "Username")
        [:div.input (text-field {:class "span6"} "username")]]
       [:div.clearfix
        (label "password" "Password")
        [:div.input (password-field {:class "span6"} "password")]]
       [:div.actions [:button.primary.btn "Login"]]])]))

(defpage [:post "/login"] {:keys [username password]}
  (str "You attempted to log in as " username " with the password " password))

(defpage "/logout" []
  (str "You have been logged out"))

(defpage "/register" []
  (common/layout
   "Sign Up"
   (form-to [:post "/register"]
     [:fieldset
      [:div.clearfix
       (label "username" "Username")
       [:div.input (text-field {:class "span6"} "username")]]
      [:div.clearfix
       (label "email" "Email")
       [:div.input (text-field {:class "span6"} "email")]]
      [:div.clearfix
       (label "password" "Password")
       [:div.input (password-field {:class "span6"} "password")]]
      [:div.clearfix
       (label "password_confirm" "Confirm Password")
       [:div.input (password-field {:class "span6"} "password_confirm")]]
      [:div.actions [:button.primary.btn "Sign Up"]]])))

(defpage "/profile" []
  (str "You have requested your profile"))
