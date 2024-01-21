(ns pert.scheduling
  (:require
   [clojure.data.priority-map :refer [priority-map]]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.set :as set]
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [hiccup2.core :as hiccup]
   [pert.csv :as csv]
   [pert.graph :as graph]
   [pert.graphviz :as graphviz]
   [pert.random-variables :as random-variables]
   ))

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

(spec/def ::project-timestamp
  (spec/and number? #(not (neg? %)) #(= % %)))

(spec/def ::start
  ::project-timestamp)

(spec/def ::end
  ::project-timestamp)

(spec/def ::interval
  (spec/and
   (spec/keys :req-un [::start ::end])
   (fn [{:keys [start end]}]
     (<= start end))
   ))

(comment
  (spec/valid? ::interval {:worker "Megan", :start 1.323462834946807, :end 2.490815823384076})
  ;; => true
  )

(defn overlap? [timeline]
  (some (fn [[previous next]]
          (< (:start next) (:end previous)))
        (partition 2 1 (sort-by :start timeline))))

(spec/def ::timeline
  (spec/coll-of ::interval))

(spec/fdef overlap?
  :args (spec/cat :timeline ::timeline))

(defn single-tasked?
  "Is the provided timeline free of any overlaps between intervals?"
  [timeline]
  (not (overlap? timeline)))

(spec/fdef single-tasked?
  :args (spec/cat :timeline ::timeline))

(spec/def ::projection
  (spec/and
   (spec/every-kv (fn [_] true) ::interval)
   (fn [p]
     (let [worker-timelines (vals (group-by :worker (vals p)))]
       (every? single-tasked? worker-timelines)))
   ))

;; spec projection
;; e.g.
(comment
  (def ex-projection
    {"d" {:worker "Megan", :start 1.323462834946807, :end 2.490815823384076},
     "f" {:worker "Megan", :start 4.490815823384076, :end 5.987796192462242},
     "e" {:worker "Megan", :start 2.490815823384076, :end 4.490815823384076},
     "j" {:worker "John", :start 12.508293580469951, :end 13.508293580469951},
     "a" {:worker "John", :start 0, :end 1.9648039531326515},
     "i" {:worker "John", :start 11.508293580469951, :end 12.508293580469951},
     "b" {:worker "John", :start 1.9648039531326515, :end 7.9421645467311635},
     "g" {:worker "Megan", :start 5.987796192462242, :end 8.508293580469951},
     "h" {:worker "John", :start 8.508293580469951, :end 11.508293580469951},
     "c" {:worker "Megan", :start 0, :end 1.323462834946807}})
  (vals (group-by :worker (vals ex-projection)))
  ([{:worker "Megan", :start 1.323462834946807, :end 2.490815823384076}
    {:worker "Megan", :start 4.490815823384076, :end 5.987796192462242}
    {:worker "Megan", :start 2.490815823384076, :end 4.490815823384076}
    {:worker "Megan", :start 5.987796192462242, :end 8.508293580469951}
    {:worker "Megan", :start 0, :end 1.323462834946807}]
   [{:worker "John", :start 12.508293580469951, :end 13.508293580469951}
    {:worker "John", :start 0, :end 1.9648039531326515}
    {:worker "John", :start 11.508293580469951, :end 12.508293580469951}
    {:worker "John", :start 1.9648039531326515, :end 7.9421645467311635}
    {:worker "John", :start 8.508293580469951, :end 11.508293580469951}])
  (spec/valid? ::projection ex-projection)
  )

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

(defn project
  "Lay out a projection of the project defined by backlog according to the durations map
  for a set of workers on the team."
  [backlog durations workers]
  (hindcast-tasks {} 0 (priority-map) backlog durations workers))

(spec/def ::duration
  ;; Looks like ::timestamp, but that's a coincidence.
  (spec/and number? #(not (neg? %)) #(= % %)))

(spec/fdef project
  :args (spec/and
         (spec/cat :backlog :pert.task/backlog
                   :durations (spec/map-of :pert.task/id ::duration)
                   :workers (spec/and set? #(< 0 (count %))))
         #((= (into #{} (map :id) (:backlog %))
              (set (keys (:durations %))))))
  :ret ::projection
  ;; :fn
  ;; TODO
  ;; ids in projection are all from the backlog
  ;; workers in projection are all from provided set of workers
  )

;; TODO: Let's test/try this out
(defn duration-sampler [backlog]
  (let [estimates (map (fn [{:keys [id estimate]}]
                         [id (random-variables/construct estimate)])
                       backlog)]
    (fn []
      (into {}
            (map (fn [[id estimate]]
                   [id (random-variables/sample estimate)]))
            estimates))))

(defn durations
  "Infinite seq of task durations sampled from the given backlog estimates."
  [backlog]
  (repeatedly (duration-sampler backlog)))

;; TODO: Let's use/test this so we don't need csv->simulator
(defn simulator
  "A project simulator for the given backlog. Invoke it with a set of workers to get a projection."
  [backlog]
  (let [sample-duration (duration-sampler backlog)]
    (fn [workers]
      (project backlog (sample-duration) workers)
      )))

;; TODO: Should be fun to try out also.
(defn simulations
  "Infinite sequence of project simulations for the given set of workers."
  [backlog workers]
  (let [simulate-project (simulator backlog)]
    (repeatedly #(simulate-project workers))))

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

(defn csv->simulator [workers in-csv]
  (let [backlog (csv/backlog in-csv)
        estimates (into {}
                        (map (fn [{:keys [id estimate]}] [id estimate]))
                        backlog)]
    #(project-record {:backlog backlog
                      :estimates estimates
                      :workers workers})))

;; TODO: (project-simulator backlog) returning a fn taking workers
;;       and returning a simulated project record.
;; TODO: Parity tests? :definitely estimates should make it reproducible.

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
   (let [backlog (csv/backlog csv-file)
         estimates (into {} (map (fn [{:keys [id estimate]}] [id estimate])) backlog)
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
