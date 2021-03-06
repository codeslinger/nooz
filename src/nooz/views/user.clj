(ns nooz.views.user
  (:require [noir.response :as resp]
            [noir.validation :as vali]
            [noir.session :as session]
            [clojure.string :as string]
            [nooz.crypto :as crypto]
            [nooz.time :as nt]
            [nooz.views.common :as common]
            [nooz.views.item :as item]
            [nooz.models.user :as user]
            [nooz.models.post :as post]
            [nooz.models.comment :as comment])
  (:use [noir.core :only [defpage
                          defpartial
                          render]]
        [hiccup.core :only [h]]
        [hiccup.page-helpers :only [link-to]]
        [hiccup.form-helpers :only [form-to
                                    label
                                    text-field
                                    text-area
                                    password-field
                                    drop-down]]))

;; ----- HELPER FUNCTIONS ----------------------------------------------------

(defn profile-link
  ([]
     (str "/user/" (session/get :username)))
  ([user]
     (str "/user/" (:username user))))

;; ----- PARTIALS ------------------------------------------------------------

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
                                       password-confirm]
                                :as user}]
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
  [:hr]
  [:div.row
   [:div.span6
    [:table.bordered-table
     [:tr
      [:td [:strong "Email"]]
      [:td (:email user)]]]]
   [:div
    (link-to {:class "side-button"}
             (str (profile-link user) "/email")
             [:button.btn.small "Update Email"])]]
  [:ul
   [:li
    (link-to (str (profile-link user) "/password")
             [:button.btn "Change password"])]])

(defpartial profile-view [{:as user}]
  (let [my-user (= (session/get :username) (:username user))]
    [:div.profile
     [:div.profile-head
      [:span.avatar [:img {:src (user/gravatar-url user)
                           :height 48
                           :width 48}]]
      [:h2 (h (:username user))]]
     [:div
      [:div.row
       (if (:about user)
         [:div.span10 [:pre (:about user)]])
       (if my-user
         [:div.span4
          (link-to (str (profile-link user) "/about")
                   [:button.btn.small "Update About"])])
       [:br]
       [:br]]]
     [:div.span6
      [:table.bordered-table
       [:tr
        [:td [:strong "Member for"]]
        [:td (nt/human-time (nt/from-long (:created_at user)))]]
       [:tr
        [:td [:strong "Submissions"]]
        (let [submissions (post/get-post-count-for-user user)]
          (if (= 0 submissions)
            [:td "0"]
            [:td (link-to (str (profile-link user) "/submissions")
                          (str submissions))]))]
       [:tr
        [:td [:strong "Comments"]]
        (let [comments (comment/get-comment-count-for-user user)]
          (if (= 0 comments)
            [:td "0"]
            [:td (link-to (str (profile-link user) "/comments")
                          (str comments))]))]]]
     (if my-user
       (profile-edit-form-parts user))]))

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

(defpartial about-form [{:keys [about] :as provo}]
  (form-to [:post (str (profile-link) "/about")]
    [:fieldset
     [(if (vali/errors? :about) :div.clearfix.error :div.clearfix)
      (label "about" "About")
      [:div.input
       (text-area {:class "span8"} "about" about)
       (vali/on-error :about common/error-inline)
       [:span.help-block "256 character maximum."]]]
     [:div.actions [:button.primary.btn "Update"]]]))

;; ----- HTTP HANDLERS -------------------------------------------------------

(defpage "/login" {:as user}
  (common/layout (login-form user)))

(defpage [:post "/login"] {:as user}
  (if (user/login! (:username user) (:password user))
    (resp/redirect "/")
    (do
      (common/boo! "There was a problem with your credentials.")
      (render "/login" user))))

(defpage "/confirm/:token" {:keys [token]}
  (let [user (user/get-user-by-token token)]
    (if user
      (do
        (user/confirm-account! user)
        (common/yay! "Your email address has been confirmed."))
      (common/boo! "This confirmation link appears to have expired."))
    (render "/login")))

(defpage "/register" {:as user}
  (if-not (session/get :username)
    (common/layout (registration-form user))
    (common/borked "You can't register while you're signed in. Logout first.")))

(defpage [:post "/register"] {:as user}
  (if (session/get :username)
    (common/borked "You can't register while you're signed in. Logout first.")
    (if (user/register! user)
      (do
        (common/yay! "We've emailed you a confirmation link. Click the link in the email to continue sign up.")
        (resp/redirect "/login"))
      (do
        (common/boo! "There was a problem with your sign up information.")
        (render "/register" user)))))

(defpage "/logout" {}
  (user/logout!)
  (resp/redirect "/"))

(defpage "/user/:username" {:keys [username]}
  (let [user (user/get-user username)]
    (common/layout (profile-view user))))

(defpage "/user/:un/email" {:as args}
  (if (= (:un args) (session/get :username))
    (common/titled-layout "Change email" (email-change-form args))
    (common/borked "You cannot modify that user's account.")))

(defpage [:post "/user/:un/email"] {:as args}
  (if-not (= (:un args) (session/get :username))
    (common/borked "You cannot modify that user's account.")
    (let [user (user/logged-in-user)]
      (if (user/update-email! user args)
        (do
          (common/yay! "Your email address has been updated.")
          (resp/redirect (profile-link)))
        (do
          (common/boo! "There was a problem updating your email address.")
          (common/layout "Change email address" (email-change-form args)))))))

(defpage "/user/:un/password" {:as args}
  (if (= (:un args) (session/get :username))
    (common/titled-layout "Change password" (password-change-form args))
    (common/borked "You cannot modify that user's account.")))

(defpage [:post "/user/:un/password"] {:as args}
  (if-not (= (:un args) (session/get :username))
    (common/borked "You cannot modify that user's account.")
    (let [user (user/logged-in-user)]
      (if (user/update-password! user args)
        (do
          (common/yay! "Your password has been updated.")
          (resp/redirect (profile-link)))
        (do
          (common/boo! "There was a problem updating your password.")
          (common/layout (password-change-form args)))))))

(defpage "/user/:un/about" {:as args}
  (if (= (:un args) (session/get :username))
    (common/titled-layout "Change about info"
                          (about-form (if (nil? (:about args))
                                        (user/logged-in-user)
                                        args)))
    (common/borked "You cannot modify that user's account.")))

(defpage [:post "/user/:un/about"] {:as args}
  (if-not (= (:un args) (session/get :username))
    (common/borked "You cannot modify that user's account.")
    (let [user (user/logged-in-user)]
      (if (user/update-about! user args)
        (do
          (common/yay! "Your information has been updated.")
          (resp/redirect (profile-link)))
        (do
          (common/boo! "There was a problem updating your information.")
          (common/layout (about-form args)))))))

(defpage "/user/:user_id/submissions" {:as args}
  (let [username (:user_id args)]
    (common/titled-layout
     (str username "'s submissions")
     (item/post-list (nt/as-long (nt/now))
                     (post/get-posts-for-user username)))))
