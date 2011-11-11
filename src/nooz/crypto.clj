(ns nooz.crypto
  (:import org.mindrot.jbcrypt.BCrypt))

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
  "Compare a raw string with an already hashed string"
  [raw hashed]
  (BCrypt/checkpw raw hashed))
