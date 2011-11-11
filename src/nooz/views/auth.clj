(ns nooz.views.auth
  (:require [noir.response :as resp]
            [noir.validation :as vali]
            [clojure.string :as string]
            [nooz.views.common :as common]
            [nooz.crypto :as crypto]
            [nooz.models.user :as user])
  (:use [noir.core :only [defpage defpartial render]]
        [hiccup.form-helpers :only [form-to
                                    label
                                    text-field
                                    password-field
                                    drop-down]]))

(defpartial error-text [errors]
  [:p.alert-message.error (string/join "<br/>" errors)])

(defpartial success-text [messages]
  [:p.alert-message.block-message.success (string/join "<br/>" messages)])

(defpartial login-form [{:keys [username] :as usr}]
  (form-to [:post "/login"]
    (vali/on-error :username error-text)
    [:fieldset
     [:div.clearfix
      (label "username" "Username")
      [:div.input (text-field {:class "span6"} "username" username)]]
     [:div.clearfix
      (label "password" "Password")
      [:div.input (password-field {:class "span6"} "password")]]
     [:div.actions [:button.primary.btn "Login"]]]))

(defpartial registration-form [{:keys [username email password password-confirm] :as user}]
  (form-to [:post "/register"]
    (vali/on-error :username error-text)
    (vali/on-error :email error-text)
    (vali/on-error :password error-text)
    [:fieldset
     [:div.clearfix
      (label "username" "Username")
      [:div.input (text-field {:class "span6"} "username" username)]]
     [:div.clearfix
      (label "email" "Email")
      [:div.input (text-field {:class "span6"} "email" email)]]
     [:div.clearfix
      (label "password" "Password")
      [:div.input (password-field {:class "span6"} "password")]]
     [:div.clearfix
      (label "password-confirm" "Confirm Password")
      [:div.input (password-field {:class "span6"} "password-confirm")]]
     [:div.actions [:button.primary.btn "Sign Up"]]]))

(defpage "/login" {:as user}
  (common/layout "Login" (login-form user)))

(defpage [:post "/login"] {:as user}
  (if (user/login! user)
    (resp/redirect "/")
    (render "/login" user)))

(defpage "/confirm" []
  (common/layout
   "Thanks!"
   [:h2 "Now, confirm your email address"]
   [:p "Once you confirm your email address, your registration will be complete. Please head to your inbox now to reply. If you don't see the confirmation mesage in a few minutes, please check your spam folder."]))

(defpage "/register" {:as user}
  (common/layout "Sign Up" (registration-form user)))

(defpage [:post "/register"] {:as user}
  (if (user/register! user)
    (resp/redirect "/confirm")
    (render "/register" user)))

(defpage "/logout" {}
  (user/logout!)
  (resp/redirect "/"))


