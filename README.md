[![Build](https://github.com/yito88/blossom/workflows/main/badge.svg)](https://github.com/yito88/blossom/actions)

# blossom
A simple Bloom filter

## Usage

```clojure
(require '[blossom.core :as blossom])

(let [bf (blossom/make-filter {})]
  (blossom/add bf "a")
  (blossom/add bf "abc")

  (prn (blossom/hit? bf "a"))    ;; -> true
  (prn (blossom/hit? bf "bc"))   ;; -> false
  (prn (blossom/hit? bf "abc"))) ;; -> true
```

- `make-filter` makes a Bloom filter with the following paramters
  ```clojure
  (blossom/make-filter {:hash-size "SHA-1"
                        :size 512
                        :num-hashes 2})
  ```
  - `hash-algo`: You can specify an algorithm for the hash function. "MD2", "MD5", "SHA-1", "SHA-224", "SHA-256", "SHA-384" or "SHA-512" can be set. By default, "SHA-256" will be set.
  - `size`: Bloom filter's size in bits. By default, the size will be 1024 bits.
  - `num-hashes`: This specifies the number of bits set to the filter when an item is added. By default, this will be 3.
