(ns nooz.views.auth
  (:require [noir.response :as resp]
            [noir.validation :as vali]
            [noir.session :as session]
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
  [:p.alert-message.success (string/join "<br/>" messages)])

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

(defpage "/confirm/:token" {:keys [token]}
  (let [user (user/get-user-by-token (crypto/unwrap-web-safe token))]
    (if (not (nil? user))
      (do
        (user/confirm-account! user)
        (session/flash-put! (success-text ["Your email address has been confirmed."])))
      (session/flash-put! (error-text ["This confirmation link appears to have expired."])))
    (render "/login")))

(defpage "/register" {:as user}
  (common/layout "Sign Up" (registration-form user)))

(defpage [:post "/register"] {:as user}
  (if (user/register! user)
    (do
      (session/flash-put! (success-text ["We've emailed you a confirmation link. Click the link in the email to continue sign up."]))
      (resp/redirect "/login"))
    (render "/register" user)))

(defpage "/logout" {}
  (user/logout!)
  (resp/redirect "/"))


