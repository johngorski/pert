(ns pert.edn
  (:require
   [clojure.edn :as edn]
   [clojure.spec.alpha :as spec]
   [pert.breakdown :as breakdown]
   [pert.task :as task]
   ))



(defmulti estimate
  "Translate EDN Estimate stanza into a random variable."
  (fn [props] (get props :type "Unknown")))


(defmethod estimate "certain"
  [{:keys [estimate]}]
  [:definitely (breakdown/coerce-num estimate)])

(defmethod estimate "gaussian"
  [{:keys [estimate standardDeviation]}]
  [:gaussian (breakdown/coerce-num estimate) (breakdown/coerce-num standardDeviation)])

(defmethod estimate "3-point"
  [{:keys [low estimate high]}]
  [:pert-3pt (breakdown/coerce-num low) (breakdown/coerce-num estimate) (breakdown/coerce-num high)])

(defmethod estimate "3-point gaussian"
  [{:keys [low estimate high]}]
  [:pert (breakdown/coerce-num low) (breakdown/coerce-num estimate) (breakdown/coerce-num high)])

(defmethod estimate "even"
  [{:keys [low high]}]
  [:uniform (breakdown/coerce-num low) (breakdown/coerce-num high)])

(defmethod estimate "Unknown"
  [_]
  [:combine-with abs :cauchy])



(defn edn-data-breakdown [edn-data]
  (:breakdown edn-data))

(defn edn-data-task-tree [edn-data]
  (:tasks (edn-data-breakdown edn-data)))

(defn edn-data-parallel [edn-data]
  (set (:parallel (edn-data-breakdown edn-data))))

(defn edn-data-details [edn-data]
  (:details edn-data))


(defn backlog-data
  "Shape data from EDN file into backlog spec"
  [edn-data]
  (let [task-tree (edn-data-task-tree edn-data)
        parallel  (edn-data-parallel edn-data)
        deps      (breakdown/remove-parallel-deps parallel (breakdown/breakdown-deps task-tree))
        ids       (breakdown/task-ids deps)
        details   (edn-data-details edn-data)]
    (->> (keys ids)
         (map (fn [task-name]
                (let [detail (get details task-name {})]
                  (cond-> {:id    (str (ids task-name))
                           :title task-name
                           :deps  (set (map (comp str ids) (get deps task-name)))}
                    (:description detail) (assoc :description (:description detail))
                    (:started detail)     (assoc :started (str (:started detail)))
                    (:finished detail)    (assoc :finished (str (:finished detail)))
                    (:estimate detail)    (assoc :estimate (estimate (:estimate detail)))))))
         (sort-by (comp edn/read-string :id)))))



(spec/fdef backlog-data
  :ret ::task/backlog)

(defn parse-backlog [edn-string]
  (->> (edn/read-string edn-string)
       backlog-data))


(defn backlog
  [edn-file]
  (-> edn-file slurp parse-backlog))


(spec/fdef parse-backlog
  :ret ::task/backlog)


(spec/fdef backlog
  :ret ::task/backlog)
