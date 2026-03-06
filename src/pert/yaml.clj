(ns pert.yaml
  (:require
   [clj-yaml.core :as yaml]
   [clojure.edn :as edn]
   [clojure.spec.alpha :as spec]
   [pert.breakdown :as breakdown]
   [pert.task :as task]
   ))



(defmulti estimate
  "Translate YAML Estimate stanza into a random variable."
  (fn [props] (get props "Type" "Unknown")))


(defmethod estimate "certain"
  [{:strs [Estimate]}]
  [:definitely (breakdown/coerce-num Estimate)])

(defmethod estimate "gaussian"
  [{:strs [Estimate StandardDeviation]}]
  [:gaussian (breakdown/coerce-num Estimate) (breakdown/coerce-num StandardDeviation)])

(defmethod estimate "3-point"
  [{:strs [Low Estimate High]}]
  [:pert-3pt (breakdown/coerce-num Low) (breakdown/coerce-num Estimate) (breakdown/coerce-num High)])

(defmethod estimate "3-point gaussian"
  [{:strs [Low Estimate High]}]
  [:pert (breakdown/coerce-num Low) (breakdown/coerce-num Estimate) (breakdown/coerce-num High)])

(defmethod estimate "even"
  [{:strs [Low High]}]
  [:uniform (breakdown/coerce-num Low) (breakdown/coerce-num High)])

(defmethod estimate "Unknown"
  [_]
  [:combine-with abs :cauchy])



(defn dep-layer     [& args] (apply breakdown/dep-layer args))
(defn dep-layers    [& args] (apply breakdown/dep-layers args))
(defn layer-dependencies [& args] (apply breakdown/layer-dependencies args))
(defn prev-deps     [& args] (apply breakdown/prev-deps args))
(defn child-deps    [& args] (apply breakdown/child-deps args))
(defn parent-tasks  [& args] (apply breakdown/parent-tasks args))
(defn all-tasks     [& args] (apply breakdown/all-tasks args))
(defn parent-deps   [& args] (apply breakdown/parent-deps args))
(defn breakdown-deps [& args] (apply breakdown/breakdown-deps args))
(defn task-ids      [& args] (apply breakdown/task-ids args))
(defn remove-parallel-deps [& args] (apply breakdown/remove-parallel-deps args))


(defn yml-data-breakdown [yml-data]
  (into {} (get yml-data "Breakdown")))

(defn yml-data-task-tree [yml-data]
  (get (yml-data-breakdown yml-data) "Tasks"))

(defn yml-data-parallel [yml-data]
  (set (get (yml-data-breakdown yml-data) "Parallel")))

(defn yml-data-details [yml-data]
  (get yml-data "Details"))


(defn backlog-data
  "Shape data from YAML file into backlog spec"
  [yml-data]
  (let [task-tree (yml-data-task-tree yml-data)
        parallel  (yml-data-parallel yml-data)
        deps      (breakdown/remove-parallel-deps parallel (breakdown/breakdown-deps task-tree))
        ids       (breakdown/task-ids deps)
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
