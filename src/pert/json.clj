(ns pert.json
  (:require
   [clojure.data.json :as json]
   [clojure.edn :as edn]
   [clojure.spec.alpha :as spec]
   [pert.breakdown :as breakdown]
   [pert.task :as task]
   ))



(defmulti estimate
  "Translate JSON Estimate stanza into a random variable."
  (fn [props] (get props "type" "Unknown")))


(defmethod estimate "certain"
  [{:strs [estimate]}]
  [:definitely (breakdown/coerce-num estimate)])

(defmethod estimate "gaussian"
  [{:strs [estimate standardDeviation]}]
  [:gaussian (breakdown/coerce-num estimate) (breakdown/coerce-num standardDeviation)])

(defmethod estimate "3-point"
  [{:strs [low estimate high]}]
  [:pert-3pt (breakdown/coerce-num low) (breakdown/coerce-num estimate) (breakdown/coerce-num high)])

(defmethod estimate "3-point gaussian"
  [{:strs [low estimate high]}]
  [:pert (breakdown/coerce-num low) (breakdown/coerce-num estimate) (breakdown/coerce-num high)])

(defmethod estimate "even"
  [{:strs [low high]}]
  [:uniform (breakdown/coerce-num low) (breakdown/coerce-num high)])

(defmethod estimate "Unknown"
  [_]
  [:combine-with abs :cauchy])



(defn json-data-breakdown [json-data]
  (get json-data "breakdown"))

(defn json-data-task-tree [json-data]
  (get (json-data-breakdown json-data) "tasks"))

(defn json-data-parallel [json-data]
  (set (get (json-data-breakdown json-data) "parallel")))

(defn json-data-details [json-data]
  (get json-data "details"))


(defn backlog-data
  "Shape data from JSON file into backlog spec"
  [json-data]
  (let [task-tree (json-data-task-tree json-data)
        parallel  (json-data-parallel json-data)
        deps      (breakdown/remove-parallel-deps parallel (breakdown/breakdown-deps task-tree))
        ids       (breakdown/task-ids deps)
        details   (json-data-details json-data)]
    (->> (keys ids)
         (map (fn [task-name]
                (let [detail (get details task-name {})]
                  (cond-> {:id    (str (ids task-name))
                           :title task-name
                           :deps  (set (map (comp str ids) (get deps task-name)))}
                    (get detail "description") (assoc :description (get detail "description"))
                    (get detail "started")     (assoc :started (str (get detail "started")))
                    (get detail "finished")    (assoc :finished (str (get detail "finished")))
                    (get detail "estimate")    (assoc :estimate (estimate (get detail "estimate")))))))
         (sort-by (comp edn/read-string :id)))))



(spec/fdef backlog-data
  :ret ::task/backlog)

(defn parse-backlog [json-string]
  (->> (json/read-str json-string)
       backlog-data))


(defn backlog
  [json-file]
  (-> json-file slurp parse-backlog))


(spec/fdef parse-backlog
  :ret ::task/backlog)


(spec/fdef backlog
  :ret ::task/backlog)
