(ns pert.core)

(defn estimate? [lo nom hi]
  (<= lo nom hi))

(defn estimate [lo nom hi]
  (when (estimate? lo nom hi)
    [lo nom hi]))

(defn mean [lo nom hi]
  (/ (+ lo (* 4 nom) hi) 6))

(defn std-dev [lo nom hi]
  (/ (- hi lo) 6))

(defn at-most-95%-confidence [m sd]
  ;; 90% of a normal distribution is within ~1.645 standard
  ;; deviations of the mean: 5% will be less than that,
  ;; 5% will be higher than that.
  (+ m (* 1.645 sd)))

(defn ete-95% [lo nom hi]
  (at-most-95%-confidence
   (mean lo nom hi)
   (std-dev lo nom hi)))

(defn combined-mean [ms]
  (reduce + ms))

(defn combined-std-dev [sds]
  (Math/sqrt (reduce + (map #(* % %) sds))))

(defn project-ete-95% [estimates]
  (at-most-95%-confidence
   (combined-mean (map #(apply mean %) estimates))
   (combined-std-dev (map #(apply std-dev %) estimates))))

