(ns nooz.mail
  (:require [nooz.crypto :as crypto]
            [postal.core :as mail])
  (:use [clojure.contrib.strint :only [<<]]
        [nooz.server :only [*app-name* *app-host*]]))

(defn registration-message [user name host]
  (let [email (:email user)
        token (crypto/wrap-web-safe (:confirmation_token user))]
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

(defn send-registration-message! [user]
  (mail/send-message
   (registration-message user *app-name* *app-host*)))