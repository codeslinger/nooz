(ns nooz.models.user
  (:require [noir.validation :as vali]
            [noir.session :as session]
            [redis.core :as redis]
            [nooz.crypto :as crypto]
            [nooz.mail :as mail]
            [nooz.time :as nt]
            [nooz.db :as db]))

(def *min-username-length* 3)
(def *max-username-length* 64)
(def *max-about-length* 256)
(def *max-email-length* 256)

(defn- user-key [username] (str "user:" username))
(defn- email-key [email] (str "email:" email))
(defn- token-key [token] (str "token:" token))

(defn get-user [username]
  (redis/with-server db/prod-redis
    (redis/hgetall (user-key username))))

(defn get-user-by-email [email]
  (let [username (redis/with-server db/prod-redis
                   (redis/get (email-key email)))]
    (if username
      (get-user username))))

(defn get-user-by-token [token]
  (let [username (redis/with-server db/prod-redis
                   (redis/get (token-key token)))]
    (if username
      (get-user username))))

(defn logged-in-user []
  (let [username (session/get :username)]
    (if username
      (get-user username))))

(defn valid-password? [password password-confirm]
  (vali/rule (vali/min-length? password 6)
             [:password "Password must be at least 6 characters."])
  (vali/rule (= password password-confirm)
             [:password "Passwords do not match."])
  (not (vali/errors? :password)))

(defn valid-new-password? [user {:keys [cur-password
                                        password
                                        password-confirm] :as provo}]
  (vali/rule (crypto/compare-hashes cur-password (get user "password"))
             [:cur-password "Current password is incorrect."])
  (valid-password? password password-confirm)
  (not (vali/errors? :cur-password :password)))

(defn valid-email? [email]
  (vali/rule (vali/max-length? email *max-email-length*)
             [:email (str "Too long. "
                          *max-email-length*
                          " characters or less, please.")])
  (vali/rule (or (vali/errors? :email)
                 (vali/is-email? email))
             [:email "Email address is in an invalid format."])
  (vali/rule (or (vali/errors? :email)
                 (not (get-user-by-email email)))
             [:email "That email address is already taken."])
  (not (vali/errors? :email)))

(defn valid-about? [about]
  (vali/rule (vali/max-length? about *max-about-length*)
             [:about (str "Too long. "
                          *max-about-length*
                          " characters or less.")])
  (not (vali/errors? :about)))

(defn valid-new-user? [{:keys [username
                               email
                               password
                               password-confirm] :as user}]
  (vali/rule (empty? (get-user username))
             [:username "That username is already taken"])
  (vali/rule (vali/min-length? username *min-username-length*)
             [:username (str "Username must be at least "
                             *min-username-length*
                             " characters.")])
  (vali/rule (vali/max-length? username *max-about-length*)
             [:username (str "Username cannot be more than "
                             *max-username-length*
                             " characters.")])
  (valid-email? email)
  (valid-password? password password-confirm)
  (not (vali/errors? :username :email :password)))

(defn- create-user-record [{:keys [username
                                   email
                                   password] :as user}]
  (let [now (nt/long-now)]
    (-> {}
        (assoc "username" username)
        (assoc "email" email)
        (assoc "password" (crypto/gen-hash password))
        (assoc "created_at" now)
        (assoc "score" 0)
        (assoc "token" (crypto/gen-confirmation-token)))))

(defn- save-user! [{:as user}]
  (let [now (nt/long-now)
        args (flatten (cons (user-key (get user "username"))
                            (seq (assoc user "updated_at" now))))]
    (apply redis/hmset args)))

(defn create-user! [{:keys [username email] :as user}]
  (let [final (create-user-record user)]
    (redis/with-server db/prod-redis
      (redis/atomically
       (save-user! final)
       (redis/set (token-key (get final "token")) username)
       (redis/set (email-key (get final "email")) username)))
    final))

(defn create-session! [username]
  (session/put! :username username))

(defn confirm-account! [{:as user}]
  (let [username (get user "username")
        token (get user "token")]
    (redis/with-server db/prod-redis
      (redis/atomically
       (redis/del (token-key token))
       (redis/hdel (user-key username) "token")))))

(defn update-password! [user provo]
  (if (valid-new-password? user provo)
    (redis/with-server db/prod-redis
      (redis/hset (user-key (get user "username"))
                  "password"
                  (crypto/gen-hash (:password provo))))))

(defn update-email! [user provo]
  (let [old-email (get user "email")
        username (get user "username")
        new-email (:email provo)]
    (if (valid-email? new-email)
      (redis/with-server db/prod-redis
        (redis/atomically
         (redis/hset (user-key username) "email" new-email)
         (redis/del (email-key old-email))
         (redis/set (email-key new-email) username))))))

(defn update-about! [user provo]
  (let [about (:about provo)
        username (get user "username")]
    (if (valid-about? about)
      (redis/with-server db/prod-redis
        (redis/hset (user-key username)
                    "about"
                    (:about provo))))))

(defn account-confirmed? [user]
  (nil? (get user "token")))

(defn login! [username password]
  (let [user (get-user username)
        cur-password (get user "password")]
    (if-not (empty? user)
      (if-not (account-confirmed? user)
        (vali/set-error
         :username
         "You need to confirm your email account before logging in. Please check your email for your confirmation link.")
        (if (crypto/compare-hashes password cur-password)
          (create-session! username)
          (vali/set-error :username "Invalid username or password.")))
      (vali/set-error :username "Invalid username or password."))))

(defn logout! []
  (session/clear!))

(defn register! [{:keys [username] :as user}]
  (if (valid-new-user? user)
    (let [created (create-user! user)]
      (mail/send-registration-message! created))))

(defn gravatar-url [user]
  (let [canonical (.toLowerCase (.trim (get user "email")))]
    (format "http://gravatar.com/avatar/%s?s=48&d=mm"
            (crypto/md5 canonical))))
