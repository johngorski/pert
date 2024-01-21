(ns pert.csv
  (:require
   [clojure.data.csv :as csv]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as sets]
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [pert.task]
   ))

(defn rows
  "Right so maybe not \"rows,\" but maybe task data? To Tasks (since everything is data)?
  That's kind of what a row is anyway, we just don't have any definition around it.
  That's what specs are for, right?"
  [filepath]
  (with-open [reader (io/reader filepath)]
    (let [rows (csv/read-csv reader)]
      (into
       []
       (doall
        (map zipmap
             (repeat (first rows))
             (rest rows)))))))

(def estimations
  {nil :pert-3pt ;; TODO: Make this [:combine-with math/abs :cauchy]
   "3-point" :pert-3pt
   "certain" :definitely
   "gaussian" :gaussian
   "3-point gaussian" :pert-gauss
   "even" :uniform
   })

(defmulti estimate
  "TODO: Parse estimates from CSV row. Make methods based on the estimation type."
  ;; see random-variables/construct methods
  ;; Also worth spec'ing these
  (fn [type props] type))

(defn- num [s] (edn/read-string s))

(defmethod estimate :definitely
  [_ {:keys [nom]}]
  [:definitely (num nom)])

(defmethod estimate :gaussian
  [_ {:keys [nom std-dev]}]
  [:gaussian (num nom) (num std-dev)])

(defmethod estimate :pert-3pt
  [_ {:keys [lo nom hi]}]
  [:pert-3pt (num lo) (num nom) (num hi)])

(defmethod estimate :pert-gauss
  [_ {:keys [lo nom hi]}]
  [:pert (num lo) (num nom) (num hi)])

(defmethod estimate :uniform
  [_ {:keys [lo hi]}]
  [:uniform (num lo) (num hi)])

(def default-task-columns
  {"ID"                 :id          ;; req
   "Title"              :title       ;; req
   "Description"        :description ;; opt
   "Dependencies"       :deps        ;; opt
   "Estimation"         :estimation  ;; opt
   "Low"                :lo          ;; opt
   "Estimate"           :nom         ;; opt
   "High"               :hi          ;; opt
   "Standard Deviation" :std-dev     ;; opt
   })

(defn parse-dependencies
  "Parse dependency cell into set of dependency IDs."
  ([s] (parse-dependencies #"\s*,\s*" s))
  ([separator s]
   (into #{}
         (filter not-empty)
         (string/split (string/trim (or s ""))
                       separator))))

(defn task
  "Project task defined by the given row.
  id, title, description, deps, estimate"
  ([row] (task {} row))
  ([column-mapper-overrides row]
   (let [column-mapper (merge default-task-columns column-mapper-overrides)
         data (sets/rename-keys row column-mapper)
         estimation (estimations (:estimation data))
         ]
     (-> (select-keys data [:id :title :description :deps])
         (update :deps parse-dependencies)
         (assoc :estimate (estimate estimation data)))
     )))

(spec/fdef task
  :ret :pert.task/task)

(defn backlog
  "Task backlog from the provided CSV file."
  ([csv-file] (backlog {} csv-file))
  ([column-overrides csv-file]
   (sequence
    (comp
     (map task)
     (remove #(empty? (:id %))))
    (rows csv-file)
    )))

(spec/fdef backlog
  :ret :pert.task/backlog)
