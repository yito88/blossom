(ns blossom.hash
  (:require [taoensso.nippy :as nippy])
  (:import (java.security MessageDigest)))

(def ^:private ^:const SALT_LENGTH 8)

(defn- get-salt
  [len]
  (->> (repeatedly #(byte (- ^int (rand-int 256) 128)))
       (take len)
       byte-array))

(defn get-hash-fn
  [algo size num-hashes]
  (let [md (MessageDigest/getInstance algo)
        salts (repeatedly num-hashes #(get-salt SALT_LENGTH))]
    (fn [item]
      (let [bs (nippy/freeze item)]
        (for [salt salts]
          (let [h (locking md (.digest md (byte-array (concat bs salt))))]
            (-> (areduce h i ret 0
                         (bit-or (bit-shift-left ret 8)
                                 (bit-and 0xff (aget h i))))
                (mod size))))))))
