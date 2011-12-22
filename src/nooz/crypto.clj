(ns nooz.crypto
  (:require [ring.util.codec :as codec]
            [clojure.string :as s]
            [clojure.contrib.str-utils :as str]
            [redis.core :as redis]
            [nooz.time :as nt]
            [nooz.db :as db])
  (:import org.mindrot.jbcrypt.BCrypt
           java.security.SecureRandom
           java.security.MessageDigest
           java.math.BigInteger
           java.util.Random))

(def ^{:private true
       :doc "Algorithm to seed random numbers."}
     seed-algorithm
     "SHA1PRNG")
(def ^{:private true} rando (new Random))
(def ^{:private true} idkey "nextid")
(def ^{:private true} epochStart 1387263000)

(defn gen-id
  "Generate a random unique ID."
  ([]
     (let [ms (- (nt/long-now) epochStart)
           r (.nextLong rando)
           id (mod (redis/with-server db/prod-redis (redis/incr idkey))
                   1024)]
       (bit-or
        (bit-or (bit-shift-left (bit-and ms 0x000001FFFFFFFFFF) 23)
                (bit-shift-left (bit-and r  0x0000000000001FFF) 10))
        (bit-and id 0x00000000000003FF)))))

(defn gen-salt
  "Generate a salt for BCrypt's hashing routines."
  ([]
     (BCrypt/gensalt))
  ([rounds]
     (BCrypt/gensalt rounds)))

(defn gen-hash
  "Hash the given string with a generated or supplied salt. Uses BCrypt hashing algorithm. The given salt (if supplied) needs to be in a format acceptable by BCrypt. Its recommended one use `gen-salt` for generating salts."
  ([raw]
     (gen-hash (gen-salt) raw))
  ([salt raw]
     (BCrypt/hashpw raw salt)))

(defn compare-hashes
  "Compare a raw string with an already hashed string."
  [raw hashed]
  (BCrypt/checkpw raw hashed))

(defn gen-random-hash
  "Returns a random byte array of the specified size."
  [size]
  (let [seed (byte-array size)]
    (.nextBytes (SecureRandom/getInstance seed-algorithm) seed)
    seed))

(defn wrap-web-safe [raw]
  (s/replace (str/re-gsub #"/" "_" (str/re-gsub #"\+" "-" raw)) "=" ""))

(defn unwrap-web-safe [wrapped]
  (let [unwrapped (str/re-gsub #"-" "+" (str/re-gsub #"_" "/" wrapped))
        eqs (seq (repeat (- 4 (rem (.length unwrapped) 4)) "="))]
    (s/join (flatten [unwrapped eqs]))))

(defn md5 [input]
  (let [alg (doto (MessageDigest/getInstance "MD5")
              (.reset)
              (.update (.getBytes input)))]
    (.toString (new BigInteger 1 (.digest alg)) 16)))

(defn gen-confirmation-token []
  (let [hashbytes (gen-random-hash 16)]
    (wrap-web-safe (codec/base64-encode hashbytes))))
