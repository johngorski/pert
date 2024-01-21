(ns pert.graph
  (:require
   [clojure.set :as sets]
   ))

(defn vertex
  [task]
  (-> task
      (select-keys [:id :title :description])
      (sets/rename-keys {:title :label, :description :tooltip})
      ))

(defn edges
  "Set of directed edges from a task to the tasks it depends on, by ID"
  [{:keys [id deps]}]
  (into #{}
        (map (fn [dep] [id dep]))
        deps
        ))

(defn graph
  ;; A thought: Do we remove redundant dependency edges?
  ;; e.g. can we simplify #{[a b] [a c] [b c]} to #{[a b] [b c]}?
  [tasks]
  {:vertices (map vertex tasks)
   :edges (reduce sets/union (map edges tasks))
   })
