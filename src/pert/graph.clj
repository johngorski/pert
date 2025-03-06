(ns pert.graph
  (:require
   [clojure.set :as sets]
   [pert.task :as t]
   ))

(def default-graph-props {})

(defn vertex
  ([task] (vertex default-graph-props task))
  ([props task]
   (let [{:keys [include-status?]} (merge default-graph-props props)
         label ((if include-status? t/title-with-status :title) task)]
     (-> task
         (select-keys [:id :title :description])
         (sets/rename-keys {:title :label, :description :tooltip})
         (assoc :label label)
         ))))

(defn vertices
  ([tasks]
   (vertex default-graph-props tasks))

  ([props tasks]
   (map (fn [task]
          (vertex props task))
        tasks)))

(defn edges
  "Set of directed edges from a task to the tasks it depends on, by ID"
  [{:keys [id deps]}]
  (into #{}
        (map (fn [dep] [id dep]))
        deps
        ))

(defn graph
  ([tasks] (graph default-graph-props tasks))
  ([props tasks]
   {:vertices (vertices props tasks)
    :edges (reduce sets/union (map edges tasks))
    }))

(defn sort-topological
  "Topological sort: https://en.wikipedia.org/wiki/Topological_sorting#Kahn's_algorithm.
  g is a graph given by an adjacency map, from task ID to set of dependency task IDs."
  ([g] (sort-topological g []))
  ([g order]
   (if (empty? g)
     order
     (let [satisfied (into []
                           (comp
                            (filter (comp empty? second))
                            (map first))
                           g)
           satisfied-set (set satisfied)]
       (when (empty? satisfied-set)
         (throw (ex-info (format "Cycle detected in task graph. No free tasks among the following: %s"
                                 (into [] (map first) g))
                         {:order order :graph g})))
       (recur (sequence (comp
                         (remove (comp satisfied-set first))
                         (map (fn [[id deps]]
                                [id (sets/difference deps satisfied-set)])))
                        g)
              (concat order satisfied))))))

(defn with-edge
  "Graph g with edge vw added."
  [g v w]
  (-> g
      (update v (fn [ws] (conj (or ws #{}) w)))
      (update w #(or % #{}))))

(defn reaching
  "The reachable set augmented such that v reaches w, and therefore reaches everything w reaches."
  [reachable-from v w]
  (update reachable-from
          v
          (fn [reachable-from-v]
            (sets/union reachable-from-v
                        (reachable-from w)
                        ))))

(defn rank
  "Lookup map from the elements in xs to the order they appear in the list."
  [xs]
  (into {} (map-indexed (fn [idx x] [x idx])) xs))

(defn transitive-reduction-edge-order
  "The order in which to examine edges when computing the transitive reduction of a graph.
  Source vertices, in topological order, with their edges in reverse topological order by their targets."
  [g]
  (let [topological-order (sort-topological g)
        reverse-topological-rank (rank (reverse topological-order))]
    (for [v topological-order
          w (sort-by reverse-topological-rank (g v))]
      [v w])))

(defn transitive-reduction
  "Transitive reduction: https://en.wikipedia.org/wiki/Transitive_reduction#Output-sensitive
  g is a graph given by an adjacency map, from task ID to set of dependency task IDs."
  [g]
  (:reduction
   (reduce (fn [{:keys [reachable-from reduction] :as current} [v w]]
             (if ((reachable-from v) w)
               current
               {:reachable-from (reaching reachable-from v w)
                :reduction (with-edge reduction v w)}))
           {:reachable-from (into {} (map (fn [v] [v #{v}])) (keys g))
            :reduction {}}
           (transitive-reduction-edge-order g)
           )))

(defn dependencies
  "Graph of task dependencies. Keys are the IDs of the tasks, values are sets of the IDs of the tasks the key task depends on."
  [tasks]
  (into {}
        (map (fn [{:keys [id deps]}] [id deps]))
        tasks))

(defn simplified
  "Simplify the project dependency graph by removing redundant edges.
  If A depends on B and C, and B depends on C, we can remove the explicit dependency A has on C."
  ([tasks] (simplified default-graph-props tasks))
  ([props tasks]
   {:vertices (vertices props tasks)
    :edges (into #{}
                 (mapcat (fn [[id deps]]
                           (map (fn [dep]
                                  [id dep])
                                deps)))
                 (transitive-reduction (dependencies tasks)))
    }))
