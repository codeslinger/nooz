(ns nooz.views.auth
  (:require [noir.response :as resp]
            [noir.validation :as vali]
            [noir.session :as session]
            [clojure.string :as string]
            [clj-time.format :as tformat]
            [clj-time.coerce :as tcoerce]
            [nooz.views.common :as common]
            [nooz.crypto :as crypto]
            [nooz.models.user :as user])
  (:use [noir.core :only [defpage
                          defpartial
                          render]]
        [hiccup.form-helpers :only [form-to
                                    label
                                    text-field
                                    password-field
                                    drop-down]]))

(defpartial login-form [{:keys [username] :as usr}]
  (form-to [:post "/login"]
    [:fieldset
     (common/form-item :username "Username" (text-field {:class "span6"} "username" username))
     (common/form-item :password "Password" (password-field {:class "span6"} "password"))
     [:div.actions [:button.primary.btn "Login"]]]))

(defpartial registration-form [{:keys [username email password password-confirm] :as user}]
  (form-to [:post "/register"]
    [:fieldset
     (common/form-item :username "Username" (text-field {:class "span6"} "username" username))
     [(if (vali/errors? :email) :div.clearfix.error :div.clearfix)
      (label "email" "Email")
      [:div.input
       (text-field {:class "span6"} "email" email)
       (vali/on-error :email common/error-inline)
       [:span.help-block "Not visible on site. Used for Gravatar icon."]]]
     (common/form-item :password "Password" (password-field {:class "span6"} "password"))
     (common/form-item :password-confirm "Confirm password" (password-field {:class "span6"} "password-confirm"))
     [:div.actions [:button.primary.btn "Sign Up"]]]))

(defpartial user-account-form [{:as user}]
  (form-to [:post "/account"]
    [:fieldset
     (common/form-item :username "Username" [:span.uneditable-input (:username user)])
     (common/form-item :email "Email" [:span.uneditable-input (:email user)])
     (common/form-item :created_at "Member since"
                       [:span.uneditable-input
                        (tformat/unparse (tformat/formatters :rfc822) (tcoerce/from-date (:created_at user)))])
     (common/form-item :cur-password "Current password" (password-field {:class "span6"} "cur-password"))
     (common/form-item :password "New password" (password-field {:class "span6"} "password"))
     (common/form-item :password-confirm "New password (again)" (password-field {:class "span6"} "password-confirm"))
     [:div.actions [:button.primary.btn "Update"]]]))

(defpage "/login" {:as user}
  (common/layout "Login" (login-form user)))

(defpage [:post "/login"] {:as user}
  (if (user/login! user)
    (resp/redirect "/")
    (do
      (common/boo! "There was a problem with your credentials.")
      (render "/login" user))))

(defpage "/confirm/:token" {:keys [token]}
  (let [user (user/get-user-by-token (crypto/unwrap-web-safe token))]
    (if (not (nil? user))
      (do
        (user/confirm-account! user)
        (common/yay! "Your email address has been confirmed."))
      (common/boo! "This confirmation link appears to have expired."))
    (render "/login")))

(defpage "/register" {:as user}
  (common/layout "Sign Up" (registration-form user)))

(defpage [:post "/register"] {:as user}
  (if (user/register! user)
    (do
      (common/yay! "We've emailed you a confirmation link. Click the link in the email to continue sign up.")
      (resp/redirect "/login"))
    (do
      (common/boo! "There was a problem with your sign up information.")
      (render "/register" user))))

(defpage "/logout" {}
  (user/logout!)
  (resp/redirect "/"))

(defpage "/account" {}
  (let [user (user/get-user-from-session)]
    (common/layout "Your Account" (user-account-form user))))

(defpage [:post "/account"] {:as provo}
  (let [user (user/get-user-from-session)]
    (if (user/update-account! user provo)
      (do
        (common/yay! "Your account information has been updated.")
        (resp/redirect "/account"))
      (do
        (common/boo! "There was a problem updating your account.")
        (render "/account" provo)))))
