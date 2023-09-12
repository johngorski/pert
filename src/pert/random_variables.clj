(ns pert.random-variables
  (:require [hiccup2.core :as hiccup])
  (:import (org.apache.commons.math3.distribution BetaDistribution)))

(def r (java.util.Random.))

(defprotocol Variable
  (sample [this]))

(defn definitely [x]
  (reify
    Variable
    (sample [this]
      x)))

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

(comment
  (sample (uniform 0 1))
  (sample (gaussian 0 1)))

(deftype D [sides]
  Variable
  (sample [this]
    (inc (rand-int sides))))

(defn x [n var]
  (apply sum-of (repeat n var)))

(comment
  (sort-by first (frequencies (repeatedly 121 #(sample (x 2 (D. 6)))))))
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

(comment
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

  (estimate 10000 (max-of (gaussian 10 4) (gaussian 11 2)))
  ;; => {:mean 12.328146230474472, :std-dev 2.4136455030297768}
  ;; => {:mean 12.328824181278293, :std-dev 2.3997693690553588}
  ;; => {:mean 12.338471902058881, :std-dev 2.405803234033849}
  ;; => {:mean 12.325619589575075, :std-dev 2.3906547089454744}
  ;; => {:mean 12.314477664216248, :std-dev 2.4036161190372667}
  ;; => {:mean 12.346640153819859, :std-dev 2.4322755424973033}
  ;; => {:mean 12.343769654161223, :std-dev 2.443541428693029}
  ;; => {:mean 12.30903267330295, :std-dev 2.396867388276872}
  ;; => {:mean 12.3690737984968, :std-dev 2.4091128179435333}
  ;; => {:mean 12.300765188801998, :std-dev 2.411493470303583}
  ;; => {:mean 12.357482816567309, :std-dev 2.4117818682221817}

  (let [dist (BetaDistribution. 2 5)
        b-rv (reify Variable (sample [_] (.sample dist)))]
    (estimate 10000 b-rv)))
;; => {:mean 0.28711780601102066, :std-dev 0.1589954521989701}

(defn pert->beta-distribution
  "Attempt from https://www.deepfriedbrainproject.com/2010/07/pert-formula.html"
  [low m high]
  (let [rt-2 (Math/sqrt 2)
        midpoint (/ (+ low high) 2)
        [alpha beta] (cond
                       (< m midpoint)
                       [(- 3 rt-2) (+ 3 rt-2)]

                       (= m midpoint)
                       [3 3]

                       (< midpoint m)
                       [(+ 3 rt-2) (- 3 rt-2)])
        dist (BetaDistribution. alpha beta)
        a low
        w (- high low)]
    (reify Variable (sample [_] (+ a (* w (.sample dist)))))))

(comment
  (defn pert->beta-distribution
    "Attempt from https://mattrauch.github.io/2019/09/21/approximation"
    [a m c]
    (let [mu (/ (+ a (* 4 m) c) 6)
          alpha (/
                 (* (- mu a) (- (* 2 m) a c))
                 (* (- m mu) (- c a)))
          beta (/
                (* alpha (- c mu))
                (- mu a))
          dist (BetaDistribution. alpha beta)
          w (- c a)]
      (reify Variable (sample [_] (+ a (* w (.sample dist))))))))

(defn pert-mean [a m b]
  (/ (+ a (* 4 m) b) 6))

(defn pert-std-dev [a m b]
  (/ (- b a) 6))

(comment
  (pert-mean 2 5 8)
  ;; => 5
  (pert-std-dev 2 5 8)
  ;; => 1

  (estimate 10000 (pert->beta-distribution 2 5 8))
  ;; => {:mean 6.421126457056413, :std-dev 1.012483566637073}
  ;; => {:mean 6.426717679870808, :std-dev 0.9899105927894863}
  ;; => {:mean 6.399932344779283, :std-dev 1.0013731731047766}
  ;; => {:mean 6.43175528907811, :std-dev 0.9932113308962013}
  ;; => {:mean 6.402619468653987, :std-dev 1.0034052293784974}
  ;; => {:mean 6.419342281439285, :std-dev 0.9976661575502696}
  ;; => {:mean 6.424355963344965, :std-dev 0.992634690677372}
  ;; => {:mean 6.416194100629757, :std-dev 1.0013918922206082}
  ;; => {:mean 6.422185625196825, :std-dev 1.0059127675929793}

  (let [rt-2 (Math/sqrt 2)
        [alpha beta] [(- 3 rt-2) (+ 3 rt-2)]
        dist (BetaDistribution. alpha beta)]
    [(.inverseCumulativeProbability dist 0)
     (.inverseCumulativeProbability dist 1)])

  (pert-mean 1 16 25)
  ;; => 15
  (pert-std-dev 1 16 25)
  ;; => 4
  (estimate 10000 (pert->beta-distribution 1 16 25))
  ;; => {:mean 18.62668299515897, :std-dev 4.036824558103441}
  ;; => {:mean 18.655541869529028, :std-dev 3.9586285577111857}
  ;; => {:mean 18.712923291879495, :std-dev 4.026197619437551}
  ;; => {:mean 18.634750831460163, :std-dev 4.0107016663173995}

  (pert-mean 1 10 25)
  ;; => 11
  (pert-std-dev 1 10 25)
  ;; => 4
  (estimate 10000 (pert->beta-distribution 1 10 25)))
;; => {:mean 7.3843963551075475, :std-dev 4.022698795625147}
;; => {:mean 7.39696720696569, :std-dev 3.980879699383923}
;; => {:mean 7.342781398608138, :std-dev 3.992918819737777}
;; => {:mean 7.333316782052228, :std-dev 4.015678577237438}

;; Means are consistently off of the text formula, standard deviations are consistently
;; accurate.
;; Seems like if we're holding alpha and beta constant then a and b may need to shift
;; slightly?
;; If we take the beta estimation shape parameters as-is, we can probably afford to shift
;; a and b based on the mean and variance...right?
;; What might have to give here is that a and b are probably a little different from the
;; optimistic and pessimistic estimates.

(defmulti construct first)
(defmethod construct :fn [[_ f & args]] (apply (combine-with f) (map construct args)))

(defmethod construct :custom [[_ var]] var)
(defmethod construct :D [[_ sides]] (D. sides))
(defmethod construct :definitely [[_ x]] (definitely x))
(defmethod construct :gaussian [[_ & args]] (apply gaussian args))
(defmethod construct :pert-gauss [[_ & args]] (apply pert args))
(defmethod construct :pert-3pt [[_ lo nom hi]] (pert->beta-distribution lo nom hi))
(defmethod construct :uniform [[_ & args]] (apply uniform args))
(defmethod construct :x [[_ n X]] (construct (concat [:fn +] (repeat n X))))

(defn interpolate-cdf
  "Returns a linearly-interpolated cumulative distribution function based on the samples.
  No fitting, just straight-line interpolation between sample points."
  [xs]
  (let [sorted (into [] (sort xs))
        n (count sorted)
        idx (fn [a x b] ;; binary search
              (let [m (+ a (quot (- b a) 2))
                    mid-left (sorted m)
                    mid-right (sorted (inc m))]
                (cond
                  (= mid-right x) (inc m)
                  (<= mid-left x mid-right) m
                  (< x mid-left) (recur a x m)
                  (<= mid-right x) (recur (inc m) x b)
                  )
                )
              )]
    (fn [x]
      (cond
        (< x (first sorted)) 0
        (<= (last sorted) x) 1
        :else (/ (inc (idx 0 x (dec n))) n)
        ))))

(defn status->rgb
  "Get RGB color values from the chance of the task starting and the chance of it finishing.
  Not started: Red (0xFF0000). In-progress: Yellow (0xFFFF00). Finished: Green (0x00FF00)."
  [chance-started chance-finished]
  [(int (Math/ceil (* 255 (- 1 chance-finished))))
   (int (Math/ceil (* 255 chance-started)))
   0])

(defn status-heights
  "Divides 1.0 based on chance started and finished. Becomes relative heights of not started/in-progress/done."
  [chance-started chance-finished]
  (let [chance-not-started (- 1.0 chance-started chance-finished)]
    [chance-not-started chance-started chance-finished]))

(defn status->svg
  "hiccup data for svg box with heights based on task status.
  Not started = red on top, in-progress = yellow in middle, done = green in remainder."
  [chance-started chance-finished]
  (let [s 20
        [r-h y-h g-h] (map (fn [x] (int (* s x))) (status-heights chance-started chance-finished))]
    [:svg
     [:rect {:x 0 :y 0 :width s :height r-h
             :style "fill:red"}] ;; not started
     [:rect {:x 0 :y r-h :width s :height y-h
             :style "fill:yellow"}] ;; in-progress
     [:rect {:x 0 :y (+ r-h y-h) :width s :height g-h
             :style "fill:green"}] ;; done
     ]
    ))

(defn task-gradient
  "Gradient based on cdfs for a task starting and task finishing."
  [start-cdf finish-cdf]
  (fn [t]
    (status->rgb (start-cdf t) (finish-cdf t))))

(defn box
  "Hiccup data for a box of color given as RGB vector on [0, 255]."
  [[r g b]]
  [:td {:style (str "background-color: rgb(" r "," g "," b ")")}])

(comment
  (str (hiccup/html
        (box [255 255 0]))))


