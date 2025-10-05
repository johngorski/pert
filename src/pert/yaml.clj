(ns pert.yaml
  (:require
   [clj-yaml.core :as yaml]
   [clojure.edn :as edn]
   [clojure.set :as set]
   [clojure.spec.alpha :as spec]
   [pert.graph :as graph]
   [pert.task :as task]
   ))



(defmulti estimate
  "Translate YAML Estimate stanza into a random variable."
  (fn [props] (get props "Type" "Unknown")))


(defn- num [s]
  (cond
    (number? s)
    (double s)

    (string? s)
    (recur (edn/read-string s))

    :default
    ##NaN
    ))


(defmethod estimate "certain"
  [{:strs [Estimate]}]
  [:definitely (num Estimate)])

(defmethod estimate "gaussian"
  [{:strs [Estimate StandardDeviation]}]
  [:gaussian (num Estimate) (num StandardDeviation)])

(defmethod estimate "3-point"
  [{:strs [Low Estimate High]}]
  [:pert-3pt (num Low) (num Estimate) (num High)])

(defmethod estimate "3-point gaussian"
  [{:strs [Low Estimate High]}]
  [:pert (num Low) (num Estimate) (num High)])

(defmethod estimate "even"
  [{:strs [Low High]}]
  [:uniform (num Low) (num High)])

(defmethod estimate "Unknown"
  [_]
  [:combine-with abs :cauchy])



;; The default is that, at the same depth, we want later tasks to depend on previous tasks.
;; The deeper depth are sub-tasks, and the outer task should depend on all subtasks.


(defn dep-layer
  "Given a list of dependencies which might be singleton maps with subtasks, list out the top-level taks."
  [dep-tree]
  (->> dep-tree
       (map (fn [node]
              (if (string? node)
                node
                ;; else map
                (first (first node)))))))


(defn dep-layers
  "arbitrarily-ordered seq of task dependency levels. It's this layer, with all of the
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


(defn yml-data-breakdown [yml-data]
  (into {} (get yml-data "Breakdown")))

(defn yml-data-task-tree [yml-data]
  (get (yml-data-breakdown yml-data) "Tasks"))

(defn yml-data-parallel [yml-data]
  (set (get (yml-data-breakdown yml-data) "Parallel")))

(defn yml-data-details [yml-data]
  (get yml-data "Details"))

(defn remove-parallel-deps
  "Remove cross-dependencies between parallel tasks."
  [parallel deps]
  (into {}
        (map (fn [[task task-deps]]
               [task (if (parallel task)
                       (set/difference task-deps parallel)
                       task-deps)]))
        deps))


(defn backlog-data
  "Shape data from YAML file into backlog spec"
  [yml-data]
  (let [task-tree (yml-data-task-tree yml-data)
        parallel  (yml-data-parallel yml-data)
        deps      (remove-parallel-deps parallel (breakdown-deps task-tree))
        ids       (task-ids deps)
        details   (yml-data-details yml-data)]
    (->> (keys ids)
         (map (fn [task-name]
                (let [detail (get details task-name {})]
                  (cond-> {:id    (str (ids task-name))
                           :title task-name
                           :deps  (set (map (comp str ids) (get deps task-name)))}
                    (get detail "Description") (assoc :description (get detail "Description"))
                    (get detail "Started")     (assoc :started (str (get detail "Started")))
                    (get detail "Finished")    (assoc :finished (str (get detail "Finished")))
                    (get detail "Estimate")    (assoc :estimate (estimate (get detail "Estimate")))))))
         (sort-by (comp edn/read-string :id)))))



(spec/fdef backlog-data
  :ret ::task/backlog)

(defn parse-backlog [yml-string]
  (->> (yaml/parse-string yml-string :keywords false)
       backlog-data))


(defn backlog
  [yml-file]
  (-> yml-file slurp parse-backlog))


(spec/fdef parse-backlog
  :ret ::task/backlog)


(spec/fdef backlog
  :ret ::task/backlog)

