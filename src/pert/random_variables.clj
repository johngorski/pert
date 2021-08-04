(ns pert.random-variables)

(def r (java.util.Random.))

(defprotocol Variable
  (sample [this]))

(defn uniform [from to]
  ;; TODO: https://clojure.org/guides/spec#_specing_functions
  (let [width (- to from)]
    (reify
      Variable
      (sample [this]
        (+ from (* width (rand)))))))

(defn gaussian [mean std-dev]
  ;; TODO: https://clojure.org/guides/spec#_specing_functions
  (reify
    Variable
    (sample [this]
      (+ mean (* std-dev (.nextGaussian r))))))

(defn pert [optimistic nominal pessimistic]
  ;; TODO: https://clojure.org/guides/spec#_specing_functions
  (let [mean (/ (+ optimistic (* 4 nominal) pessimistic) 6)
        std-dev (/ (- pessimistic optimistic) 6)]
    (gaussian mean std-dev)))

(defn combine-with [f]
  (fn [& vars]
    (reify
      Variable
      (sample [this]
        (apply f (map sample vars))))))

(def sum-of (combine-with +))
(def max-of (combine-with max))
(def min-of (combine-with min))

(sample (uniform 0 1))

(sample (gaussian 0 1))

(deftype D [sides]
  Variable
  (sample [this]
    (inc (rand-int sides))))

(defn x [n var]
  (apply sum-of (repeat n var)))

(sort-by first (frequencies (repeatedly 121 #(sample (x 2 (D. 6))))))
;; => ([2 5] [3 4] [4 9] [5 10] [6 15] [7 24] [8 15] [9 14] [10 14] [11 6] [12 5])
;; => ([2 8] [3 13] [4 10] [5 14] [6 20] [7 17] [8 17] [9 9] [10 7] [11 5] [12 1])
;; => ([2 3] [3 7] [4 11] [5 14] [6 17] [7 21] [8 11] [9 16] [10 11] [11 7] [12 3])
;; => ([2 5] [3 10] [4 11] [5 10] [6 23] [7 16] [8 12] [9 13] [10 11] [11 5] [12 5])
;; => ([2 3] [3 7] [4 9] [5 15] [6 17] [7 24] [8 12] [9 15] [10 8] [11 8] [12 3])
;; => ([2 2] [3 6] [4 9] [5 12] [6 16] [7 17] [8 16] [9 17] [10 14] [11 7] [12 5])
;; => ([2 3] [3 8] [4 6] [5 14] [6 19] [7 13] [8 21] [9 15] [10 8] [11 11] [12 3])
;; => ([2 5] [3 8] [4 14] [5 11] [6 21] [7 24] [8 15] [9 7] [10 8] [11 5] [12 3])
;; => ([2 5] [3 7] [4 7] [5 16] [6 14] [7 16] [8 28] [9 12] [10 9] [11 4] [12 3])
;; => ([2 4] [3 4] [4 6] [5 12] [6 16] [7 21] [8 16] [9 12] [10 13] [11 11] [12 6])
;; => ([2 2] [3 7] [4 4] [5 18] [6 24] [7 22] [8 14] [9 14] [10 11] [11 3] [12 2])
;; => ([2 1] [3 7] [4 12] [5 11] [6 14] [7 28] [8 17] [9 14] [10 6] [11 7] [12 4])

(defn estimate [n var]
  (let [samples (repeatedly n #(sample var))
        mean (/ (reduce + samples) n)
        variance (/
                  (reduce + (map #(let [diff (- mean %)]
                                    (* diff diff))
                                 samples))
                  (dec n))]
    {:mean mean
     :std-dev (Math/sqrt variance)}))

(estimate 10000 (gaussian 4 3))
;; => {:mean 4.020923898459046, :std-dev 2.9969052751018124}
;; => {:mean 3.9518470171049436, :std-dev 3.0320527853405266}
;; => {:mean 4.045326830030188, :std-dev 3.0031900872048145}
;; => {:mean 3.9711829877877323, :std-dev 3.012916514541721}
;; => {:mean 3.9557327732322696, :std-dev 2.9600958199340917}

(estimate 10000 (sum-of (gaussian 4 3) (gaussian 6 4)))
;; => {:mean 10.01493985297808, :std-dev 5.010778799829074}
;; => {:mean 9.935628205491945, :std-dev 4.980924043657126}
;; => {:mean 9.870373430115997, :std-dev 4.97871378915859}
;; => {:mean 9.932173626888822, :std-dev 4.990704352433281}
;; => {:mean 10.040386546448817, :std-dev 4.987096863024789}

(estimate 10000 (x 2 (D. 6)))
;; => {:mean 17369/2500, :std-dev 2.4313464139316827}

(estimate 10000 (uniform 0 1))
;; => {:mean 0.4990765240630893, :std-dev 0.28737847522825544}
