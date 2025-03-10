(ns pert.spreadsheet
  (:require
   [clojure.edn :as edn]
   [clojure.set :as sets]
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [pert.task]))


(defn rows [raw-rows]
  (into
   []
   (doall
    (map zipmap
         (repeat (map string/trim (first raw-rows)))
         (rest raw-rows)))))


(def estimations
  {nil :pert-3pt ;; TODO: Make this [:combine-with math/abs :cauchy]
   "3-point" :pert-3pt
   "certain" :definitely
   "gaussian" :gaussian
   "3-point gaussian" :pert-gauss
   "even" :uniform})


(defmulti estimate
  "TODO: Parse estimates from CSV row. Make methods based on the estimation type."
  ;; see random-variables/construct methods
  ;; Also worth spec'ing these
  (fn [type props] type))


(defn- num [s]
  (cond
    (number? s)
    (double s)

    (string? s)
    (recur (edn/read-string s))

    :default
    ##NaN
    ))


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
   "Started"            :started     ;; opt
   "Finished"           :finished    ;; opt
   })


(defn parse-dependencies
  "Parse dependency cell into set of dependency IDs."
  ([s] (parse-dependencies #"\s*,\s*" s))
  ([separator s]
   (into #{}
         (filter not-empty)
         (string/split (string/trim (or s ""))
                       separator))))


(defn dissoc-empty
  "Remove k from m when it holds merely an empty string."
  [m k]
  (if-let [v (m k)]
    (if (and (string? v) (empty? (string/trim v)))
      (dissoc m k)
      m)
    m))


(comment
  (dissoc-empty {:started ""} :started)
  ;; => {}
  (dissoc-empty {:started "1/1/2000"} :started)
  ;; => {:started "1/1/2000"}
  (dissoc-empty {:started (java.util.Date. "1/1/2000")} :started)
  (dissoc-empty {:started #inst "2000-01-01"} :started)
  ())


(defn task
  "Project task defined by the given row.
  id, title, description, deps, estimate"
  ([row] (task {} row))
  ([column-mapper-overrides row]
   (let [column-mapper (merge default-task-columns column-mapper-overrides)
         data (sets/rename-keys row column-mapper)
         estimation (estimations (:estimation data))]
     (-> (select-keys data [:id :title :description :deps :started :finished])
         (update :id str)
         (dissoc-empty :started)
         (dissoc-empty :finished)
         (update :deps parse-dependencies)
         (assoc :estimate (estimate estimation data))))))

(spec/fdef task
  :ret :pert.task/task)


(defn backlog
  "Task backlog from the provided task spreadsheet rows."
  ([rows] (backlog {} rows))
  ([column-overrides rows]
   (sequence
    (comp
     (map (partial task column-overrides))
     (remove #(string/blank? (:id %)))
     )
    rows)))

(spec/fdef backlog
  :ret :pert.task/backlog)
