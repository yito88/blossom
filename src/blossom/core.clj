(ns blossom.core
  (:require [blossom.hash :as hash]))

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

(defrecord BloomFilter [hash-fn array]
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

(defrecord ConcurrentBloomFilter [hash-fn array]
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
  (if thread-safe?
    (->> (get-initial-ref-array size)
         (->ConcurrentBloomFilter (hash/get-hash-fn hash-algo size
                                                    num-hashes)))
    (->> (get-initial-array size)
         (->BloomFilter (hash/get-hash-fn hash-algo size num-hashes)))))
