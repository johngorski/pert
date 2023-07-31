(ns pert.scheduling
  (:require
   [clojure.data.csv :as csv]
   [clojure.data.priority-map :refer [priority-map]]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.set :as set]
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [pert.random-variables :as random-variables]))

;; Why find a closed form for the random variables representing schedule
;; estimates when you can simulate instead?

;; The point here is to do this a lot for a project--say, 10,000 times. Well, even then that's
;; a step towards what we really want, which is a schedule we can convey to stakeholders with
;; decent confidence built up from the (relative) certainties in work schedules and
;; uncertainties in how well our task estimates will bear out in practice. We get that by
;; simple stats: We can read out the cdf directly from an ordered list, we can do order
;; statistics to find p95 completion times, and we can calculate the simulated means and
;; variances and then lean on the Central Limit Theorem (unless and until we have a reason to
;; believe some distribution other than the normal distribution is both more appropriate and
;; feasible to estimate based on the sample).

;; This is great for reporting mid-project, too: i.e., regular status reports. Done tasks are
;; done, they can't hurt anybody any more. Update estimates for any in-progress tasks to
;; account for the expected work remaining. Update backlog estimates with any hard lessons
;; you've learned about optimism bias while delivering. Stakeholders are over-the-moon with
;; ETAs that are being met week over week, so have fun basking in the glow of their admiration.

;; For an additional "wow" factor, represent the distributions for which the ETAs are
;; 95%-confidence estimates as pdfs, cdfs, color gradients, etc. Maybe some box-and-whisker plots.
;; Probably some box-and-whisker plots as inputs to some fancy gradients. Yeah.
;; Since the CLT really just applies for ~30 events from similar distributions, box-and-whisker
;; will be a better basis for the visualization for smaller projects than sample mean and variance.

(defn available-based-on
  "Predicate returning whether a backlog task has its dependencies fulfilled by the given record."
  [record]
  (let [completed (into #{} (keys record))]
    (fn [{:keys [deps]}]
      (empty? (set/difference deps completed)))))

(defn assign-idle-workers
  "Updated in-progress and backlog tasks when idle workers are assigned."
  [record now in-progress backlog durations workers]
  (let [idle-workers (reduce disj workers (map :worker (keys in-progress)))
        available-tasks (filter (available-based-on record) backlog)
        assignments (map (fn [worker task] {:worker worker, :task task})
                         idle-workers
                         available-tasks)
        in-progress' (into in-progress
                           (map (fn [{:keys [worker task]}]
                                  (let [id (:id task)
                                        duration (get durations id)
                                        end (+ now duration)]
                                    [{:worker worker
                                      :task id
                                      :start now}
                                     end])))
                           assignments)
        assigned-tasks (into #{} (map (comp :id :task)) assignments)
        backlog' (remove #(assigned-tasks (:id %)) backlog)]
    {:in-progress' in-progress'
     :backlog' backlog'}))

(defn record-finished-tasks
  ""
  [record now in-progress]
  (let [[finished-tasks started-tasks]
        (partition-by (fn [[_ end]] (<= end now)) (seq in-progress))

        record' (into record
                      (map (fn [[{:keys [worker task start]} end]]
                             [task {:worker worker, :start start, :end end}]))
                      finished-tasks)
        in-progress' (into (priority-map) started-tasks)]
    {:record' record'
     :in-progress' in-progress'}))

(defn hindcast-tasks
  "With task durations already known, lay them out."
  [record now in-progress backlog durations workers]
  (comment
    (pprint {:record record
             :now now
             :in-progress in-progress
             :backlog backlog
             :durations durations
             :workers workers}))
  (cond
    ;; If the backlog's empty and nothing's in-progress, we're done.
    (and (empty? backlog) (empty? in-progress))
    record

    ;; If there are idle workers that can be assigned, assign them.
    (and (some (available-based-on record) backlog)
         (let [idle-workers (reduce disj workers (map :worker (keys in-progress)))]
           (not-empty idle-workers)))
    (let [{:keys [in-progress' backlog']}
          (assign-idle-workers record now in-progress backlog durations workers)]
      (recur record now in-progress' backlog' durations workers))

    ;; If tasks are finished, record them.
    (some (fn [[_ end]] (<= end now)) in-progress)
    (let [{:keys [record' in-progress']} (record-finished-tasks record now in-progress)]
      (recur record' now in-progress' backlog durations workers))

    ;; If everyone's busy with in-progress work, advance to the next finished task.
    :else
    (let [later (-> in-progress first second)]
      (recur record later in-progress backlog durations workers))))

(defn project-record
  "A simulated record of the given project."
  [{:keys [backlog estimates workers]
    :or {workers #{"Employee"}}}]
  (let [task-durations (map (fn [{:keys [id]}]
                              (let [estimate (random-variables/construct (get estimates id))]
                                [id (random-variables/sample estimate)]))
                            backlog)]
    (hindcast-tasks {} 0 (priority-map) backlog (into {} task-durations) workers)))

(defn worker-timelines
  "Takes a project record mapping tasks to worker/start/end and flips it to mapping workers to
  the sorted sequence of tasks."
  [record]
  (->> (seq record)
       (map (fn [[task-id completion-record]]
              (assoc completion-record :task task-id)))
       (group-by :worker)
       seq
       (map (fn [[worker tasks]]
              [worker (sort-by :start (map #(dissoc % :worker) tasks))]))
       (into {})))

(defn parse-dependencies
  "Parse dependency cell into set of dependency IDs."
  ([s] (parse-dependencies #"\s*,\s*" s))
  ([separator s] (into #{} (filter not-empty) (string/split s separator))))

(defn csv->rows
  [filepath]
  (with-open [reader (io/reader filepath)]
    (let [rows (doall (csv/read-csv reader))]
      (map zipmap
           (repeat (first rows))
           (rest rows)))))

(defn rows->backlog
  "Sequence of rows to a project backlog. First row is a header row. 2-arity flavor includes a translation map."
  ([rows] (rows->backlog {:id "ID", :deps "Dependencies"} rows))
  ([{:keys [id deps]} rows]
   (->> rows
        (map (fn [row]
               {:id (get row id)
                :deps (parse-dependencies (get row deps))})))))

(defn backlog->dependencies
  "Set of directed edges from a task to the tasks it depends on, by ID"
  [backlog]
  (into #{}
        (mapcat (fn [{:keys [id deps]}]
                  (map (fn [dep] [id dep]) deps)))
        backlog))

(defn rows->vertices
  ""
  ([rows]
   (rows->vertices
    {:id "ID", :title "Title", :description "Description"}
    rows))
  ([{:keys [id title description]} rows]
   (map
    (fn [row]
      [(get row id) {:label (get row title (get row id)), :tooltip (get row description)}])
    rows)
   ))

(defn rows->3pt-estimates
  ([rows] (rows->3pt-estimates {:id "ID", :lo "Low", :nom "Estimate", :hi "High"} rows))
  ([{:keys [id lo nom hi]} rows]
   (into {}
         (map (fn [row]
                [(get row id)
                 (cons :pert-3pt
                       (map
                        (fn [n] (edn/read-string (get row n)))
                        [lo nom hi]))])
         rows))))

(comment
  (with-open [reader (io/reader "test/example.csv")]
    (backlog->dependencies (rows->backlog (csv/read-csv reader))))
  (rows->vertices (csv->rows "test/example.csv"))
  (rows->3pt-estimates (csv->rows "test/example.csv"))
  )

;; Neat stuff! What are some useful things to estimate, once we have task dependencies and estimates?
;; 1. ETA/total cost
;; 2. Changes to ETA as parallelism increases/decreases
;; 3. ETA for a specific task as parallelism changes
;; 4. ETA as scope changes/alternative designs (implicit to creating a single project)

;; We should be able to estimate the ETA by looking at the max end date of a task.
;; If that task didn't need to be completed for the ETA, it's out of scope.
;; We might also want to estimate the ETA of a specific task.
;; The rest is interesting at a nerdy/detailed level, but I haven't been asked for that level of
;; detail ever.

;; Simple enough: Let's define the total time for a project as the max value of an end date of a task
;; based on its breakdown and estimates. From there, we can estimate away.

;; Turns out we don't seem at this point to need the neato combinatorics of the original estimation
;; engine for the scope of our question, but who knows?

(defn csv->ETE
  "Takes a CSV file of project dependencies and 3-pt estimates and returns a random variable
  representing the completion time of the last task"
  ([csv-file] (csv->ETE 1 csv-file))
  ([worker-count csv-file]
   (let [rows (csv->rows csv-file)
         backlog (rows->backlog rows)
         estimates (rows->3pt-estimates rows)
         random-record (reify
                         random-variables/Variable
                         (sample [_]
                           (project-record {:backlog backlog
                                            :estimates estimates
                                            :workers (set (range worker-count))})))]
     ((random-variables/combine-with
       (fn [record]
         (->> record
              vals
              (map :end)
              (apply max)
              )))
      random-record))))

