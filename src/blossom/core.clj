(ns blossom.core
  (:require [blossom.hash :as hash])
  (:import (java.io ByteArrayInputStream
                    ByteArrayOutputStream
                    ObjectInputStream
                    ObjectOutputStream)))

(def ^:private ^:const BITS_PER_LONG 64)

(defn- decode
  [hash-fn item]
  (->> (hash-fn item)
       (map (fn [^long p]
              [(quot p BITS_PER_LONG) (mod p BITS_PER_LONG)]))))

(defprotocol IBloomFilter
  (add [this item])
  (hit? [this item]))

(defn- get-initial-array
  [size]
  (-> (/ size BITS_PER_LONG) Math/ceil long-array))

(defrecord BloomFilter [hash-fn salts array]
  IBloomFilter
  (add [_ item]
    (let [bit-positions (decode hash-fn item)]
      (doseq [[index offset] bit-positions]
        (->> offset
             (bit-set (aget array index))
             (aset-long array index)))))

  (hit? [_ item]
    (let [bit-positions (decode hash-fn item)]
      (every? (fn [[index offset]]
                (-> (aget array index) (bit-test offset)))
              bit-positions))))

(defn- get-initial-ref-array
  [size]
  (-> (/ size BITS_PER_LONG)
      Math/ceil
      (take (repeatedly #(ref 0)))
      to-array))

(defrecord ConcurrentBloomFilter [hash-fn salts array]
  IBloomFilter
  (add [_ item]
    (let [bit-positions (decode hash-fn item)]
      (doseq [[index offset] bit-positions]
        (dosync
         (let [l (aget array index)]
           (->> (bit-set @l offset)
                (ref-set l)))))))

  (hit? [_ item]
    (let [bit-positions (decode hash-fn item)]
      (every? (fn [[index offset]]
                (-> @(aget array index) (bit-test offset)))
              bit-positions))))

(defn make-filter
  "Makes a new bloom filter."
  [{:keys [hash-algo size num-hashes thread-safe?]
    :or {hash-algo "SHA-256" size 1024 num-hashes 3 thread-safe? false}}]
  (assert (pos? size) "The size should be positive!")
  (let [salts (hash/get-salts num-hashes)
        hash-fn (hash/get-hash-fn hash-algo size salts)]
    (if thread-safe?
      (->> (get-initial-ref-array size)
           (->ConcurrentBloomFilter hash-fn salts))
      (->> (get-initial-array size)
           (->BloomFilter hash-fn salts)))))

(defn serialize-filter
  "Serialize the salts and the bit-array."
  [bloom-filter]
  (let [byte-stream (ByteArrayOutputStream.)
        salts (:salts bloom-filter)
        ref? (->> bloom-filter :array first (instance? clojure.lang.Ref))
        array (if ref?
                (->> bloom-filter :array (mapv deref))
                (:array bloom-filter))]
    (with-open [obj-stream (ObjectOutputStream. byte-stream)]
      (.writeObject obj-stream [salts array]))
    (.toByteArray byte-stream)))

(defn restore-filter
  "Restores the bloom filter from the salts and the bit-array."
  [salts array
   {:keys [hash-algo size num-hashes thread-safe?]
    :or {hash-algo "SHA-256" size 1024 num-hashes 3 thread-safe? false}}]
  (assert (pos? size) "The size should be positive!")
  (assert (= (count salts) num-hashes)
          "The size of the salts mismatched the num-hashes!")
  (let [hash-fn (hash/get-hash-fn hash-algo size salts)]
    (if thread-safe?
      (->ConcurrentBloomFilter hash-fn salts (->> array (map ref) to-array))
      (->BloomFilter hash-fn salts array))))

(defn deserialize-filter
  "Deserialize the salt and the bit array. It returns [salts array]."
  [bytes params]
  (let [byte-stream (ByteArrayInputStream. bytes)
        [salts array] (with-open [obj-stream (ObjectInputStream. byte-stream)]
                        (.readObject obj-stream))]
    (restore-filter salts array params)))
