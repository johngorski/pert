(ns pert.task
  (:require
   [clojure.set :as sets]
   [clojure.spec.alpha :as spec]
   ))

(spec/def ::id
  (some-fn string? keyword?))

(spec/def ::title
  string?)

(spec/def ::description
  string?)

(spec/def ::deps
  (spec/and set? (spec/coll-of ::id)))

(spec/def ::task
  (spec/and
   (spec/keys
    :req-un [::id ::title ::deps]
    :opt-un [::description])
   (fn [{:keys [id deps]}]
     (not (deps id)))))

;; - estimate: one of available estimate specs. Multi-specs might be the ideal here.

(defn dependencies
  "Map of task IDs to their dependency IDs."
  [backlog]
  (into {}
        (map (fn [{:keys [id deps]}] [id deps]))
        backlog
        ))

(comment
  (set (keys {:a 1 :b 2}))
  ;; => #{:b :a}
  (set (vals {:a 1 :b 2}))
  ;; => #{1 2}
  )

(spec/def ::backlog
  (spec/and
   (spec/coll-of ::task)
   (fn [backlog]
     (let [deps (dependencies backlog)]
       (and
        (= (count backlog)
           (count deps))
        (empty? (sets/difference
                 (reduce sets/union (vals deps))
                 (set (keys deps))))
        )))))
;; - but also,
;;   - no cycles in task dependencies

(comment
  (spec/fdef dependencies
    :args (spec/tuple ::backlog)
    :ret))
