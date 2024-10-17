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

(defn date-str?
  "Really there's more to it than just being a string, but I'd rather borrow than build that."
  [s]
  (string? s))

(spec/def ::started date-str?)
(spec/def ::finished date-str?)

(def states #{:to-do :in-progress :done})

(spec/def ::task
  (spec/and
   (spec/keys
    :req-un [::id ::title ::deps]
    :opt-un [::description ::started ::finished])
   (fn [{:keys [id deps]}]
     (not (deps id)))))

(defn status [task]
  (let [{:keys [started finished]} task]
    (if started
      (if finished
        :done
        :in-progress)
      :to-do)))

(defn status-symbol [status]
  ({:to-do "📋"
    :in-progress "🔧"
    :done "✅"
    } status))

(defn title-with-status [task]
  (str (status-symbol (status task)) " " (:title task)))

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
