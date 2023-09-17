(ns blossom.hash-test
  (:require [clojure.test :refer :all]
            [blossom.hash :as h]))

(deftest get-hash-fn-test
  (let [salts (h/get-salts 2)
        f (h/get-hash-fn "SHA-256" 32 salts)]
    (is (= (f "aaa") (f "aaa")))
    (is (= 2 (count (f "aaa"))))
    (is (every? #(< ^long % 32) (f "aaa")))))
