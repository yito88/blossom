[![Clojars Project](https://img.shields.io/clojars/v/blossom.svg)](https://clojars.org/blossom)
[![Build](https://github.com/yito88/blossom/workflows/build/badge.svg)](https://github.com/yito88/blossom/actions)

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
  (blossom/make-filter {:hash-size "SHA-256"
                        :size 1024
                        :num-hashes 3
                        :thread-safe? false})
  ```
  - `hash-algo`: You can specify an algorithm for the hash function. "MD2", "MD5", "SHA-1", "SHA-224", "SHA-256", "SHA-384" or "SHA-512" can be set. By default, "SHA-256" will be set.
  - `size`: Bloom filter's size in bits. By default, the size will be 1024 bits.
  - `num-hashes`: This specifies the number of bits set to the filter when an item is added. By default, this will be 3.
  - `thread-safe?`: If this is true, a thread-safe filter is made. The performance of the filter will be lower than that of a filter with `{:thread-safe false}`. BY default, it will be false.
