(ns nooz.models.user
  (:require [noir.validation :as vali]
            [noir.session :as session]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [nooz.crypto :as crypto]
            [nooz.db :as db]
            [nooz.mail :as mail])
  (:use korma.core)
  (:import java.sql.Timestamp))

(def *min-username-length* 3)
(def *max-username-length* 64)
(def *max-about-length* 256)
(def *max-email-length* 256)

(defn get-user-by-name [username]
  (first
   (select db/users
     (where {:username username})
     (limit 1))))

(defn get-user-by-email [email]
  (first
   (select db/users
     (where {:email email})
     (limit 1))))

(defn get-user-by-token [token]
  (first
   (select db/users
     (where {:confirmation_token token})
     (limit 1))))

(defn get-user-from-session []
  (let [user (get-user-by-name (session/get :username))]
    user))

(defn get-name-for-id [id]
  (let [row (select db/users
              (fields :username)
              (where {:id id})
              (limit 1))]
    (if row
      (:username (first row)))))

(defn valid-password? [password password-confirm]
  (vali/rule (vali/min-length? password 6)
             [:password "Password must be at least 6 characters."])
  (vali/rule (= password password-confirm)
             [:password "Passwords do not match."])
  (not (vali/errors? :password)))

(defn valid-new-password? [user {:keys [cur-password password password-confirm] :as provo}]
  (vali/rule (crypto/compare-hashes cur-password (:password user))
             [:cur-password "Current password is incorrect."])
  (valid-password? password password-confirm)
  (not (vali/errors? :cur-password :password)))

(defn valid-email? [email]
  (vali/rule (vali/max-length? email *max-email-length*)
             [:email (str "Too long. " *max-email-length* " characters or less, please.")])
  (vali/rule (or (vali/errors? :email)
                 (vali/is-email? email))
             [:email "Email address is in an invalid format."])
  (vali/rule (or (vali/errors? :email)
                 (not (get-user-by-email email)))
             [:email "That email address is already taken."])
  (not (vali/errors? :email)))

(defn valid-about? [about]
  (vali/rule (vali/max-length? about *max-about-length*)
             [:about (str "Too long. " *max-about-length* " characters or less.")])
  (not (vali/errors? :about)))

(defn valid-new-user? [{:keys [username email password password-confirm] :as user}]
  (vali/rule (not (get-user-by-name username))
             [:username "That username is already taken"])
  (vali/rule (vali/min-length? username *min-username-length*)
             [:username (str "Username must be at least " *min-username-length* " characters.")])
  (vali/rule (vali/max-length? username *max-about-length*)
             [:username (str "Username cannot be more than " *max-username-length* " characters.")])
  (valid-email? email)
  (valid-password? password password-confirm)
  (not (vali/errors? :username :email :password)))

(defn create-user! [{:keys [username email password] :as user}]
  (let [created_at (Timestamp. (coerce/to-long (time/now)))]
    (insert db/users
      (values {:username username
               :email email
               :password (crypto/gen-hash password)
               :created_at created_at
               :confirmation_token (crypto/gen-confirmation-token)}))))

(defn create-session! [username]
  (session/put! :username username))

(defn confirm-account! [user]
  (update db/users
    (set-fields {:confirmation_token "*confirmed*"})
    (where {:id [= (:id user)]})))

(defn update-password! [user provo]
  (if (valid-new-password? user provo)
    (update db/users
      (set-fields {:password (crypto/gen-hash (:password provo))})
      (where {:id [= (:id user)]}))))

(defn update-email! [user provo]
  (let [email (:email provo)]
    (if (valid-email? email)
      (update db/users
        (set-fields {:email email})
        (where {:id [= (:id user)]})))))

(defn update-about! [user provo]
  (let [about (:about provo)]
    (if (valid-about? about)
      (update db/users
        (set-fields {:about about})
        (where {:id [= (:id user)]})))))

(defn login! [{:keys [username password] :as user}]
  (let [user (get-user-by-name username)]
    (if (nil? user)
      (vali/set-error :username "Invalid username or password.")
      (if (not (= "*confirmed*" (:confirmation_token user)))
        (vali/set-error :username "You need to confirm your email account before logging in. Please check your email for your confirmation link.")
        (if (crypto/compare-hashes password (:password user))
          (create-session! username)
          (vali/set-error :username "Invalid username or password."))))))

(defn logout! []
  (session/clear!))

(defn register! [{:as user}]
  (if (valid-new-user? user)
    (let [username (:username user)]
      (create-user! user)
      (mail/send-registration-message! (get-user-by-name username)))))

(defn gravatar-url [user]
  (format "http://gravatar.com/avatar/%s?s=48&d=mm"
          (crypto/md5 (.toLowerCase (.trim (:email user))))))
