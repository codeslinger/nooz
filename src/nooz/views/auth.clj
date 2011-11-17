(ns nooz.views.auth
  (:require [noir.response :as resp]
            [noir.validation :as vali]
            [noir.session :as session]
            [clojure.string :as string]
            [clj-time.core :as time]
            [clj-time.format :as tformat]
            [clj-time.coerce :as tcoerce]
            [nooz.views.common :as common]
            [nooz.crypto :as crypto]
            [nooz.models.user :as user])
  (:use [noir.core :only [defpage
                          defpartial
                          render]]
        [hiccup.core :only [h]]
        [hiccup.page-helpers :only [link-to]]
        [hiccup.form-helpers :only [form-to
                                    label
                                    text-field
                                    password-field
                                    drop-down]]))

(defn profile-link
  ([]
     (str "/user/" (session/get :username)))
  ([user]
     (str "/user/" (:username user))))

(defpartial login-form [{:keys [username] :as usr}]
  (form-to [:post "/login"]
    [:fieldset
     (common/form-item :username "Username"
                       (text-field {:class "span6"} "username" username))
     (common/form-item :password "Password"
                       (password-field {:class "span6"} "password"))
     [:div.actions [:button.primary.btn "Login"]]]))

(defpartial registration-form [{:keys [username
                                       email
                                       password
                                       password-confirm] :as user}]
  (form-to [:post "/register"]
    [:fieldset
     (common/form-item :username "Username"
                       (text-field {:class "span6"} "username" username))
     [(if (vali/errors? :email) :div.clearfix.error :div.clearfix)
      (label "email" "Email")
      [:div.input
       (text-field {:class "span6"} "email" email)
       (vali/on-error :email common/error-inline)
       [:span.help-block "Not visible on site. Used for Gravatar icon."]]]
     (common/form-item :password "Password"
                       (password-field {:class "span6"} "password"))
     (common/form-item :password-confirm "Confirm password"
                       (password-field {:class "span6"} "password-confirm"))
     [:div.actions [:button.primary.btn "Sign Up"]]]))

(defpartial profile-edit-form-parts [{:as user}]
  [:div.row
   [:div.span5
    [:table.bordered-table
     [:tr
      [:td [:strong "Email"]]
      [:td (:email user)]]]]
   [:div
    (link-to {:class "email-button"}
             (str (profile-link user) "/email")
             [:button.btn.small "Update"])]]
  [:ul
   [:li (link-to (str (profile-link user) "/password")
                 [:button.btn "Change password"])]])

(defpartial profile-view [{:as user}]
  [:div.profile
   [:div.profile-head
    [:span.avatar [:img {:src (user/gravatar-url user)
                         :height 48
                         :width 48}]]
    [:h2 (h (:username user))]]
   (if (:about user)
     [:pre (:about user)])
   [:div.span5
    [:table.bordered-table
     [:tr
      [:td [:strong "Member for"]]
      [:td (common/human-time (:created_at user))]]
     [:tr
      [:td [:strong "Submissions"]]
      [:td (link-to (str (profile-link user) "/submissions") "0")]]
     [:tr
      [:td [:strong "Comments"]]
      [:td (link-to (str (profile-link user) "/comments") "0")]]]]
   (if (= (session/get :username) (:username user))
     (profile-edit-form-parts user))])

(defpartial email-change-form [{:keys [email] :as user}]
  (form-to [:post (str (profile-link) "/email")]
    [:fieldset
     [(if (vali/errors? :email) :div.clearfix.error :div.clearfix)
      (label "email" "New email")
      [:div.input
       (text-field {:class "span6"} "email" email)
       (vali/on-error :email common/error-inline)
       [:span.help-block "Not visible on site. Used for Gravatar icon."]]
     [:div.actions [:button.primary.btn "Update"]]]]))

(defpartial password-change-form [{:as provo}]
  (form-to [:post (str (profile-link) "/password")]
    [:fieldset
     (common/form-item :cur-password "Current password"
                       (password-field {:class "span6"} "cur-password"))
     (common/form-item :password "New password"
                       (password-field {:class "span6"} "password"))
     (common/form-item :password-confirm "New password (again)"
                       (password-field {:class "span6"} "password-confirm"))
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

(defpage "/user/:username" {:keys [username]}
  (let [user (user/get-user-by-name username)]
    (common/layout "Profile" (profile-view user))))

(defpage [:get "/user/:un/email"] {:as args}
  (if (= (:un args) (session/get :username))
    (common/layout "Change email address" (email-change-form args))
    (common/borked "You cannot modify that user's account.")))

(defpage [:post "/user/:un/email"] {:as args}
  (if-not (= (:un args) (session/get :username))
    (common/borked "You cannot modify that user's account.")
    (let [user (user/get-user-from-session)]
      (if (user/update-email! user args)
        (do
          (common/yay! "Your email address has been updated.")
          (resp/redirect (profile-link)))
        (do
          (common/boo! "There was a problem updating your email address.")
          (common/layout "Change email address" (email-change-form args)))))))

(defpage "/user/:un/password" {:as args}
  (if (= (:un args) (session/get :username))
    (common/layout "Change password" (password-change-form args))
    (common/borked "You cannot modify that user's account.")))

(defpage [:post "/user/:un/password"] {:as args}
  (if-not (= (:un args) (session/get :username))
    (common/borked "You cannot modify that user's account.")
    (let [user (user/get-user-from-session)]
      (if (user/update-password! user args)
        (do
          (common/yay! "Your password has been updated.")
          (resp/redirect (profile-link)))
        (do
          (common/boo! "There was a problem updating your password.")
          (common/layout "Change password" (password-change-form args)))))))
