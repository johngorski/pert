(ns pert.breakdown
  "Format-agnostic task breakdown logic: dependency computation, topological
  ordering, and ID assignment. Shared by yaml, json, and edn parsers."
  (:refer-clojure :exclude [num])
  (:require
   [clojure.edn :as edn]
   [clojure.set :as set]
   [pert.graph :as graph]
   ))


(defn- num [s]
  (cond
    (number? s)
    (double s)

    (string? s)
    (recur (edn/read-string s))

    :default
    ##NaN
    ))


(defn coerce-num
  "Coerce a value to double, handling strings and numbers."
  [s]
  (num s))


(defn dep-layer
  "Given a list of dependencies which might be singleton maps with subtasks, list out the top-level tasks."
  [dep-tree]
  (->> dep-tree
       (map (fn [node]
              (if (string? node)
                node
                ;; else map
                (first (first node)))))))


(defn dep-layers
  "Arbitrarily-ordered seq of task dependency levels. It's this layer, with all of the
  layers following."
  [dep-tree]
  (cons
   (dep-layer dep-tree)
   (sequence
    (comp
     (filter map?)
     (mapcat (fn [parent-task]
            (-> parent-task
                vals
                first
                dep-layers
                )
            )))
    dep-tree)))


(defn layer-dependencies
  "Dependencies from the single layer, where later items depend on previous."
  [tasks]
  (zipmap
   tasks
   (reductions (fn [deps t]
                 (conj deps t))
               #{}
               tasks)))


(defn prev-deps
  "Dependencies based on previous tasks being dependencies of later tasks at the same layer."
  [task-tree]
  (apply merge-with
         set/union
         (map layer-dependencies (dep-layers task-tree))))


(defn child-deps
  "Subtasks of a task are dependencies of that task's parent."
  [task-tree]
  (apply merge
         (sequence
          (comp
           (filter map?)
           (mapcat (fn [parent-task]
                     (let [parent-id (first (keys parent-task))
                           subtasks (get parent-task parent-id)]
                       (cons
                        {parent-id (set (dep-layer subtasks))}
                        (child-deps subtasks)))
                     )))
          task-tree)))


(defn parent-tasks
  "Map from a subtask to its parent task"
  [task-tree]
  (apply merge
         (sequence
          (comp
           (filter map?)
           (mapcat (fn [parent-task]
                     (let [parent-id (first (keys parent-task))
                           subtasks (get parent-task parent-id)]
                       (concat
                        (map (fn [subtask]
                               {subtask parent-id})
                             (dep-layer subtasks))
                        (parent-tasks subtasks))))))
          task-tree)))


(defn all-tasks
  "Seq of all tasks in the given task tree"
  [task-tree]
  (apply concat (dep-layers task-tree)))


(defn parent-deps
  "Child dependencies have all the pre-dependencies of their parents."
  [task-tree]
  (let [prev (prev-deps task-tree)
        parents (parent-tasks task-tree)
        tasks (all-tasks task-tree)]
    (into {}
          (comp
           (filter parents)
           (map (fn [task]
                  [task (get prev (get parents task))]
                  )))
          tasks)))


(defn breakdown-deps
  [task-tree]
  (apply merge-with set/union
         ((juxt prev-deps
                child-deps)
          task-tree)))


(defn task-ids
  "Map from task name to sequential integer ID, starting from 1, in topological order of deps."
  [deps]
  (into {}
        (map-indexed (fn [idx task] [task (inc idx)]))
        (graph/sort-topological deps)))


(defn remove-parallel-deps
  "Remove cross-dependencies between parallel tasks."
  [parallel deps]
  (into {}
        (map (fn [[task task-deps]]
               [task (if (parallel task)
                       (set/difference task-deps parallel)
                       task-deps)]))
        deps))
