(ns nooz.models.user
  (:require [nooz.crypto :as crypto]
            [noir.validation :as vali]
            [noir.cookies :as cookie]
            [clj-time.core :as time]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [nooz.db :as db])
  (:use korma.core)
  (:import java.sql.Timestamp))

(def *secret-key* "DeAdBeEf")

(defn get-user [id]
  (first (select db/users
           (where {:id id})
           (limit 1))))

(defn get-user-by-name [username]
  (first (select db/users
           (where {:username username})
           (limit 1))))

(defn get-user-by-email [email]
  (first (select db/users
           (where {:email email})
           (limit 1))))

(defn create-user [{:keys [username email password] :as user}]
  (let [created_at (Timestamp. (tc/to-long (time/now)))]
    (insert db/users
      (values {:username username
               :email email
               :password password
               :created_at created_at}))))

(defn valid? [{:keys [username email password password-confirm] :as user}]
  (vali/rule (not (get-user-by-name username))
             [:username "That username is already taken"])
  (vali/rule (vali/min-length? username 3)
             [:username "Username must be at least 3 characters."])
  (vali/rule (vali/is-email? email)
             [:email "Email address is in an invalid format."])
  (vali/rule (not (get-user-by-email email))
             [:email "That email address is already taken."])
  (vali/rule (vali/min-length? password 6)
             [:password "Password must be at least 6 characters."])
  (vali/rule (= password password-confirm)
             [:password "Passwords do not match."])
  (not (vali/errors? :username :email :password)))

(defn create-session [username]
  (cookie/put-signed! *secret-key* :nooz username))

(defn destroy-session []
  (cookie/put! :nooz ""))

(defn get-user-from-session []
  (let [username (cookie/get-signed *secret-key* :nooz)]
    (if (nil? username)
      nil
      (get-user-by-name username))))

(defn get-session-username []
  (let [username (cookie/get-signed *secret-key* :nooz)]
    username))

(defn is-logged-in? []
  (get-session-username))

(defn logout! []
  (destroy-session))

(defn login! [{:keys [username password] :as user}]
  (let [user (get-user-by-name username)]
    (if (and user
             (crypto/compare-hashes password (get user :password)))
      (create-session username)
      (vali/set-error :username "Invalid username or password"))))

(defn register! [{:as user}]
  (if (valid? user)
    (do
      (create-user user)
      (create-session (get user :username)))
    nil))
