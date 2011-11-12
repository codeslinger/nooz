(ns nooz.crypto
  (:require [ring.util.codec :as codec])
  (:import org.mindrot.jbcrypt.BCrypt
           java.security.SecureRandom))

(def ^{:private true
       :doc "Algorithm to seed random numbers."}
     seed-algorithm
     "SHA1PRNG")

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

(defn gen-confirmation-token []
  (let [hashbytes (gen-random-hash 16)]
    (codec/url-encode (codec/base64-encode hashbytes))))
