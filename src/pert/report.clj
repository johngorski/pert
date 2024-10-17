(ns pert.report
  (:require
   [clojure.set :as sets]
   [pert.gantt :as gantt]
   [pert.task :as t]))

(defn ecd-day [gantt-data task-id percentile]
  (let [chart (get gantt-data task-id)

        [day-idx completion-fraction]
        (first
         (sequence
          (comp
           (map-indexed (fn [day-idx {:keys [queued in-progress finished]}]
                          [day-idx (/ finished (+ queued in-progress finished))]))
           (drop-while (fn [day-idx completion-fraction]
                         (< completion-fraction percentile)))
           (take 1))
          chart))]
    {:ecd-day-idx day-idx
     :completion-fraction completion-fraction}))

(defn status-report
  "Needed stuff:
   - backlog
   - gantt chart projection days
   - todo: finished tasks should have zero'd-out estimates
  For dates:
   - starting day/day 0
   - holidays (besides weekends)
   - In practice, what we really want is a map from day index to date. Mmmaybe vice-versa.
  "
  [backlog]
  (cons
   ["Task" "Started" "Finished" "p95 ECD"]
   (map (juxt
         #(t/title-with-status %)
         #(get % :started "")
         #(get % :finished "")
         (fn [task]
           (if (:finished task)
             ""
             (let [task-id (:id task)]
               "TBD"))))
        backlog)))

