(ns blossom.core-test
  (:require [clojure.test :refer :all]
            [blossom.core :as blossom]))

(defn rand-str
  [len]
  (->> (repeatedly #(char (+ (rand 26) 65)))
       (take len)
       (apply str)))

(deftest filter-simple-test
  (let [bf (blossom/make-filter {})]
    (blossom/add bf "a")
    (blossom/add bf "abc")
    (blossom/add bf "1234")

    (is (true? (blossom/hit? bf "a")))
    (is (false? (blossom/hit? bf "bc")))
    (is (true? (blossom/hit? bf "1234")))))

(deftest filter-many-test
  (let [bf (blossom/make-filter {:size 10240})
        inputs (take 1000 (repeatedly #(rand-str (rand-int 256))))]
    (doseq [i inputs]
      (blossom/add bf i))
    (is (true? (every? #(blossom/hit? bf %) inputs)))))
