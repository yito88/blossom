(ns blossom.core
  (:require [blossom.hash :as hash]))

;; TODO: concurrently add items

(def ^:private ^:const BITS_PER_BYTE 8)

(defn- decode
  [hash-fn item]
  (->> (hash-fn item)
       (map (fn [^long p]
              [(quot p BITS_PER_BYTE) (mod p BITS_PER_BYTE)]))))

(defn- get-initial-array
  [^long size]
  (-> (/ size BITS_PER_BYTE) Math/ceil byte-array))

(defprotocol IBloomFilter
  (add [this item])
  (hit? [this item]))

(defrecord BloomFilter [hash-fn array]
  IBloomFilter
  (add [_ item]
    (let [bit-positions (decode hash-fn item)]
      (doseq [[index offset] bit-positions]
        (->> offset
             (bit-set (aget array index))
             unchecked-byte
             (aset-byte array index)))))

  (hit? [_ item]
    (let [bit-positions (decode hash-fn item)]
      (every? (fn [[index offset]]
                (-> (aget array index) (bit-test offset)))
              bit-positions))))

(defn make-filter
  "Makes a new bloom filter."
  [{:keys [hash-algo ^long size num-hashes]
    :or {hash-algo "SHA-256" size 1024 num-hashes 3}}]
  (assert (pos? size) "The size should be positive!")
  (->> (get-initial-array size)
       (->BloomFilter (hash/get-hash-fn hash-algo size num-hashes))))
