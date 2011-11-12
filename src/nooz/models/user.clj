(ns nooz.models.user
  (:require [noir.validation :as vali]
            [noir.session :as session]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [postal.core :as mail]
            [nooz.crypto :as crypto]
            [nooz.db :as db])
  (:use korma.core
        [clojure.contrib.strint :only [<<]]
        [nooz.server :only [*app-name* *app-host*]])
  (:import java.sql.Timestamp))

(defn get-user-by-name [username]
  (first (select db/users (where {:username username}) (limit 1))))

(defn get-user-by-email [email]
  (first (select db/users (where {:email email}) (limit 1))))

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

(defn create-user! [{:keys [username email password] :as user}]
  (let [created_at (Timestamp. (coerce/to-long (time/now)))]
    (insert db/users
      (values {:username username
               :email email
               :password password
               :created_at created_at
               :confirmation_token (crypto/gen-confirmation-token)}))))

(defn create-session! [username]
  (session/put! :username username))

(defn login! [{:keys [username password] :as user}]
  (let [user (get-user-by-name username)]
    (if (and user
             (crypto/compare-hashes password (get user :password)))
      (create-session! username)
      (vali/set-error :username "Invalid username or password"))))

(defn logout! []
  (session/clear!))

(defn registration-message [user name host]
  (let [email (:email user)
        token (:confirmation_token user)]
    {:from (<< "help@~{host}")
     :to email
     :subject (<< "~{host}: New ~{name} Account ~{email}")
     :body (<< "Thank you for signing up for a ~{name} account.

To log in to your account, please go to:

http://~{host}/confirm/~{token}

Your account will be confirmed and you will be prompted to log in.

If you experience any problems or have any questions, please reply to
this message or email help@~{host}.

Thank you for using ~{name}!")}))

(defn send-registration-confirmation! [user]
  (mail/send-message (registration-message user
                                           *app-name*
                                           *app-host*)))

(defn register! [{:as user}]
  (if (valid? user)
    (do
      (create-user! user)
      (create-session! (:username user))
      (send-registration-confirmation! (get-user-by-name (:username user))))
    nil))