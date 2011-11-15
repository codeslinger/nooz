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

(defn valid-new-user? [{:keys [username email password password-confirm] :as user}]
  (vali/rule (not (get-user-by-name username))
             [:username "That username is already taken"])
  (vali/rule (vali/min-length? username 3)
             [:username "Username must be at least 3 characters."])
  (vali/rule (vali/is-email? email)
             [:email "Email address is in an invalid format."])
  (vali/rule (not (get-user-by-email email))
             [:email "That email address is already taken."])
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

(defn update-account! [user provo]
  (if (valid-new-password? user provo)
    (update db/users
      (set-fields {:password (crypto/gen-hash (:password provo))})
      (where {:id [= (:id user)]}))))

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
