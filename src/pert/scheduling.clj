(ns pert.scheduling
  (:require
   [clojure.data.priority-map :refer [priority-map]]
   [clojure.pprint :refer [pprint]]
   [clojure.set :as set]
   [clojure.spec.alpha :as spec]
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
      (recur record now in-progress' backlog' durations workers))

    ;; If tasks are finished, record them.
    (some (fn [[_ end]] (<= end now)) in-progress)
    (let [[finished-tasks started-tasks]
          (partition-by (fn [[_ end]] (<= end now)) (seq in-progress))

          record' (into record
                        (map (fn [[{:keys [worker task start]} end]]
                               [task {:worker worker, :start start, :end end}]))
                        finished-tasks)
          in-progress' (into (priority-map) started-tasks)]
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
