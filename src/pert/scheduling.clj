(ns pert.scheduling
  (:require
   [clojure.data.priority-map :refer [priority-map]]
   [clojure.set :as set]
   [clojure.spec.alpha :as spec]
   [pert.random-variables :as random-variables]))

;; Why find a closed form for the random variables representing schedule
;; estimates when you can simulate instead?

;; Input:
;; Seq of tasks in priority order
;; Task: ID, dependencies, estimate
;; Despendencies: set of task IDs which must complete before the current one
;; Estimate: Random Variable for estimating the duration of the task
;; Number of workers
;; Start date

;; Output:
;; Schedule

;; In PMP-speak, I suppose this would be a PERT chart -> Gantt chart converter?
;; https://open.lib.umn.edu/exploringbusiness/chapter/11-4-graphical-tools-pert-and-gantt-charts/

;; Start:
;; - Sort forests of tasks topologically by strategic priority of value-yielding goals.
;;   Probably some initial guess based on critical path based on the PERT chart.
;;   Like, tasks that will shorten the critical path the most upon their completion.
;;   Although, you could imagine a short task which could unlock several other critical-path
;;   tasks which would allow for the flexibility of additional workers.
;; - Clock starts from the starting time.
;; - Assign idle workers to unencumbered tasks sorted by
;;   - (when idle workers will remain) maximum next-task fanout
;;   - Sorted priority order
;; - Assume single-focused workers: They're working on their assigned task to completion.
;; - Simulate completion time of assigned tasks. This is the efficient moment to take vacation
;;   into account.
;; - Advance the clock to the minimum time the first task is completed
;; - The worker who completed that task is now free and needs a new assignment. If more than one
;;   task is then available, other idle workers may be assigned as well for the next round.
;; - Assign these unassigned workers at this stop time and simulate their competion times.
;;   Remember other workers may have tasks in-progress!
;; - Rinse and repeat: The clock only needs to advance to the minimum completion time at each
;;   step. From there, that worker is idle, the assigned task is comple fte, assign idle workers
;;   to remaining unencumbered tasks, simulate the completion times, advance to minimum
;;   completion time.
;; This is a super-imperative way to describe this but somehow I'm still expecting good things
;; to happen from coding this declaratively. Go-go Gadget Clojure.
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

(defn bear-pert []
  ;; https://open.lib.umn.edu/exploringbusiness/chapter/11-4-graphical-tools-pert-and-gantt-charts/
  (map (fn [[id deps lo nom hi]]
         {:id id
          :deps deps
          :estimate (random-variables/pert->beta-distribution lo nom hi)})
       [["cut cloth" #{} 9 10 11]
        ["cut fur" #{} 8 10 12]
        ["cut accessories" #{} 4 5 7]
        ["sew clothes" #{"cut cloth"} 9 10 14]
        ["stuff fur" #{"cut fur"} 25 30 31]
        ["sew accessories" #{"cut accessories"} 9 10 15]
        ["dress bear" #{"cut cloth" "cut fur" "cut accessories"} 14 15 17]
        ["package bear" #{"dress bear"} 4 5 6]
        ["ship bear" #{"package bear"} 4 5 5]]))

(defn in-progress-entry [clock done worker {:keys [id estimate]}]
  [{:worker worker
    :task id
    :start clock}
   (+ clock (random-variables/sample estimate))])

(defn idle-assignments
  "Returns new in-progress maps. Kept as priority-maps to conveniently get the latest completed."
  [{:keys [clock ;; numeric logical clock
           idle-workers ;; set of unassigned workers
           to-do ;; sequence of tasks: {id, deps, estimate}
           in-progress ;; priority-map from {task, worker, start} to logical finish time
           done] ;; set of completed task ids
    :as args}]
  (into in-progress (map (partial clock done)
                         idle-workers
                         (remove (fn [{:keys [deps]}]
                                   (empty? (set/difference deps done)))
                                 to-do))))

(comment
  (idle-assignments
   {:clock 0
    :idle-workers (shuffle #{"John" "Megan"})
    :to-do (bear-pert)
    :in-progress (priority-map)
    :done #{}}))
;; => {{:worker "John", :task "sew clothes", :start 0} 11.390350665041307, {:worker "Megan", :task "stuff fur", :start 0} 28.68972849583643}
;; => {{:worker "John", :task "sew clothes", :start 0} 10.243040861916828, {:worker "Megan", :task "stuff fur", :start 0} 29.046522341290764}
;; => {{:worker "John", :task "sew clothes", :start 0} 10.330139316101839, {:worker "Megan", :task "stuff fur", :start 0} 28.016374519528473}
;; => {{:worker "John", :task "sew clothes", :start 0} 9.953478625320736, {:worker "Megan", :task "stuff fur", :start 0} 29.801026824712928}

(defn ticked-clock [{:keys [in-progress]}]
  (second (first in-progress)))

(defn completed-started [{:keys [in-progress]} clock']
  (partition-by
   (fn [[_ finish]] (<= finish clock'))
   in-progress))

(defn completed-done [{:keys [done]} completed]
  (into done (map (comp :task first)) completed))

(defn relieved-workers [{:keys [idle-workers]} completed]
  (into idle-workers (map (comp :worker first)) completed))

(defn left-to-do [{:keys [to-do]} in-progress']
  (let [in-progress-tasks (into #{} (map (comp :task first)) in-progress')]
    (remove #(in-progress-tasks (:task (first %))) to-do)))

(defn fast-forward
  "Advance to next completed task. New clock time, new assignments for idle workers (including the one that just finished)."
  [{:keys
    [clock ;; numeric logical clock
     idle-workers ;; set of unassigned workers
     to-do ;; sequence of tasks: {id, deps, estimate}
     in-progress ;; priority-map from {task, worker, start} to logical finish time
     done ;; set of completed task ids
     record] ;; record of what's done. Keys are the tasks, values are start/end/who.
    :as args}]
  (let [clock' (ticked-clock args)
        [completed started] (completed-started args clock')
        done' (completed-done args completed)
        ;; to-do' (remove #(done' (:task (first %))) to-do) ;; <= Updated to-do should be based on what's now in-progress...
        idle-workers' (relieved-workers args completed)
        in-progress' (idle-assignments {:clock clock'
                                        :idle-workers idle-workers'
                                        :to-do to-do
                                        :in-progress (into (priority-map) started)
                                        :done done'})
        to-do' (left-to-do args in-progress')
        report (into {}
                     (map
                      (fn [[{:keys [worker task start]} finish]]
                        [task {:start start, :finish finish, :worker worker}]))
                     completed)
        ]
    {:clock clock'
     :idle-workers idle-workers'
     :to-do to-do'
     :done done'
     :in-progress in-progress'
     :record (merge record report)}))

(fast-forward
 {:clock 0
  :idle-workers #{}
  :to-do []
  :in-progress (priority-map {:worker "John", :task "sew clothes", :start 0} 12.121751940946131,
                             {:worker "Megan", :task "stuff fur", :start 0} 27.609489894389352)
  :done #{}})

(comment
  {:clock 12.121751940946131,
   :idle-workers #{"John"},
   :to-do (),
   :done #{"sew clothes"},
   :in-progress {{:worker "Megan", :task "stuff fur", :start 0} 27.609489894389352},
   :record {"sew clothes" {:start 0, :finish 12.121751940946131, :worker "John"}}}
  )

(bear-pert)
;; => ({:id "cut cloth", :deps #{}, :estimate #object[pert.random_variables$pert$reify__1968 0x76a29526 "pert.random_variables$pert$reify__1968@76a29526"]} {:id "cut fur", :deps #{}, :estimate #object[pert.random_variables$pert$reify__1968 0x2917f0e0 "pert.random_variables$pert$reify__1968@2917f0e0"]} {:id "cut accessories", :deps #{}, :estimate #object[pert.random_variables$pert$reify__1968 0x2ff5e069 "pert.random_variables$pert$reify__1968@2ff5e069"]} {:id "sew clothes", :deps #{"cut cloth"}, :estimate #object[pert.random_variables$pert$reify__1968 0x22418bc "pert.random_variables$pert$reify__1968@22418bc"]} {:id "stuff fur", :deps #{"cut fur"}, :estimate #object[pert.random_variables$pert$reify__1968 0x604db67b "pert.random_variables$pert$reify__1968@604db67b"]} {:id "sew accessories", :deps #{"cut accessories"}, :estimate #object[pert.random_variables$pert$reify__1968 0x764bda62 "pert.random_variables$pert$reify__1968@764bda62"]} {:id "dress bear", :deps #{"cut accessories" "cut cloth" "cut fur"}, :estimate #object[pert.random_variables$pert$reify__1968 0x763d9a6a "pert.random_variables$pert$reify__1968@763d9a6a"]} {:id "package bear", :deps #{"dress bear"}, :estimate #object[pert.random_variables$pert$reify__1968 0x54fe8de9 "pert.random_variables$pert$reify__1968@54fe8de9"]} {:id "ship bear", :deps #{"package bear"}, :estimate #object[pert.random_variables$pert$reify__1968 0x590d1763 "pert.random_variables$pert$reify__1968@590d1763"]})

(random-variables/sample (:estimate (first (bear-pert))))

(let [args {:clock 0
            :idle-workers #{"John" "Megan"}
            :to-do (bear-pert)
            :in-progress (priority-map)
            :done #{}}]
  (ticked-clock args))
;; => nil ;; <= well, there ya go, that's probably related!
(let [{:keys [clock idle-workers to-do in-progress done] :as args} {:clock 0
                                                                    :idle-workers #{"John" "Megan"}
                                                                    :to-do (bear-pert)
                                                                    :in-progress (priority-map)
                                                                    :done #{}}
      clock' (ticked-clock args)
      [completed started] (completed-started args clock')
      done' (completed-done args completed)
      ;; to-do' (remove #(done' (:task (first %))) to-do) ;; <= Updated to-do should be based on what's now in-progress...
      idle-workers' (relieved-workers args completed)
      in-progress-args {:clock clock'
                        :idle-workers idle-workers'
                        :to-do to-do
                        :in-progress (into (priority-map) started)
                        :done done'}
      ;; in-progress' (idle-assignments in-progress-args)
      ;; to-do' (left-to-do args in-progress')
      ;; report (into {}
      ;; (map
      ;; (fn [[{:keys [worker task start]} finish]]
      ;; [task {:start start, :finish finish, :worker worker}]))
      ;; completed)
      ]
  in-progress-args)

(comment
  {:clock nil
   :idle-workers #{"John" "Megan"}
   :to-do ({:id "cut cloth"
            :deps #{}
            :estimate :e}
           {:id "cut fur", :deps #{}, :estimate :e}
           {:id "cut accessories", :deps #{}, :estimate :e}
           {:id "sew clothes", :deps #{"cut cloth"}, :estimate :e}
           {:id "stuff fur", :deps #{"cut fur"}, :estimate :e}
           {:id "sew accessories", :deps #{"cut accessories"}, :estimate :x}
           {:id "dress bear", :deps #{"cut accessories" "cut cloth" "cut fur"}, :estimate :x}
           {:id "package bear", :deps #{"dress bear"}, :estimate :x}
           {:id "ship bear", :deps #{"package bear"}, :estimate :x})
   :in-progress {}
   :done #{}})

(comment
  (idle-assignments {:clock nil
                     :idle-workers #{"John" "Megan"}
                     :to-do [{:id "cut cloth"
                              :deps #{}
                              :estimate (random-variables/pert->beta-distribution 9 10 11)}
                             {:id "cut fur", :deps #{}, :estimate (random-variables/pert->beta-distribution 9 10 11)}
                             {:id "cut accessories", :deps #{}, :estimate (random-variables/pert->beta-distribution 9 10 11)}
                             {:id "sew clothes", :deps #{"cut cloth"}, :estimate (random-variables/pert->beta-distribution 9 10 11)}
                             {:id "stuff fur", :deps #{"cut fur"}, :estimate (random-variables/pert->beta-distribution 9 10 11)}
                             {:id "sew accessories", :deps #{"cut accessories"}, :estimate (random-variables/pert->beta-distribution 9 10 11)}
                             {:id "dress bear", :deps #{"cut accessories" "cut cloth" "cut fur"}, :estimate (random-variables/pert->beta-distribution 9 10 11)}
                             {:id "package bear", :deps #{"dress bear"}, :estimate (random-variables/pert->beta-distribution 9 10 11)}
                             {:id "ship bear", :deps #{"package bear"}, :estimate (random-variables/pert->beta-distribution 9 10 11)}]
                     :in-progress {}
                     :done #{}}))

(comment 
  (fast-forward
   {:clock 0
    :idle-workers #{"John" "Megan"}
    :to-do (bear-pert)
    :in-progress (priority-map)
    :done #{}}))


(defn pert->gantt-step
  ""
  [{:keys
    [clock ;; numeric logical clock
     idle-workers ;; set of unassigned workers
     to-do ;; sequence of tasks: {id, deps, estimate}
     in-progress ;; priority-map from {task, worker, start} to logical finish time
     done ;; set of completed task ids
     ]
    :as args}]
  (let [idle-assignments (idle-assignments args)]
    (cond
      (not-empty idle-assignments)
      ;; continue with new worker assignments
      (let [assigned-tasks (into #{}
                                 (map :task)
                                 (keys idle-assignments))]
        (recur
         (assoc args
                :idle-workers (set/difference
                               idle-workers
                               (map :worker (keys idle-assignments)))
                :to-do (remove
                        (fn [{:keys [id]}] (assigned-tasks id))
                        to-do)
                :in-progress (merge in-progress idle-assignments))))

      (not-empty in-progress)
      ;; fast-forward to next completed task
      (let [{:keys [clock' completed report]} (fast-forward args)]
        (concat
         report
         (lazy-seq
          (pert->gantt-step
           (assoc args
                  :clock clock'
                  :idle-workers (into idle-workers (map :worker) completed)
                  :in-progress (apply dissoc in-progress (map first completed))
                  :done (into done (map :id) completed))))))

      :else
      ;; We're blocked?
      args)))

(defn pert->gantt
  "clock: the time at this step in the simulation
  to-do: unstarted tasks
  in-progress: map from workers to in-progress tasks with start and completion times (or :bye)
  done: "
  [workers to-do]
  (pert->gantt-step
   {:clock 0
    :workers workers
    :to-do to-do
    :in-progress (priority-map)
    :done #{}}))

(pert->gantt #{"John" "Megan"} (bear-pert))
(comment
  {:clock 0,
   :workers #{"John" "Megan"},
   ;; :to-do ({:id "cut cloth",
   ;; :deps #{}
   ;; :estimate #object[pert.random_variables$pert$reify__1968 0x69ce37bf "pert.random_variables$pert$reify__1968@69ce37bf"]}
   ;; {:id "cut fur"
   ;; :deps #{}
   ;; :estimate #object[pert.random_variables$pert$reify__1968 0x6791657d "pert.random_variables$pert$reify__1968@6791657d"]}
   ;; {:id "cut accessories"
   ;; :deps #{}
   ;; :estimate #object[pert.random_variables$pert$reify__1968 0x24bd99e6 "pert.random_variables$pert$reify__1968@24bd99e6"]}
   ;; {:id "sew clothes"
   ;; :deps #{"cut cloth"}
   ;; :estimate #object[pert.random_variables$pert$reify__1968 0x304cfb76 "pert.random_variables$pert$reify__1968@304cfb76"]}
   ;; {:id "stuff fur"
   ;; :deps #{"cut fur"}
   ;; :estimate #object[pert.random_variables$pert$reify__1968 0x28a7410f "pert.random_variables$pert$reify__1968@28a7410f"]}
   ;; {:id "sew accessories"
   ;; :deps #{"cut accessories"}
   ;; :estimate #object[pert.random_variables$pert$reify__1968 0x44f163eb "pert.random_variables$pert$reify__1968@44f163eb"]}
   ;; {:id "dress bear"
   ;; :deps #{"cut accessories" "cut cloth" "cut fur"}
   ;; :estimate #object[pert.random_variables$pert$reify__1968 0xc4bb03a "pert.random_variables$pert$reify__1968@c4bb03a"]}
   ;; {:id "package bear"
   ;; :deps #{"dress bear"}
   ;; :estimate #object[pert.random_variables$pert$reify__1968 0x5b8b7d8e "pert.random_variables$pert$reify__1968@5b8b7d8e"]}
   ;; {:id "ship bear"
   ;; :deps #{"package bear"}
   ;; :estimate #object[pert.random_variables$pert$reify__1968 0x49b6f0c9 "pert.random_variables$pert$reify__1968@49b6f0c9"]}),
   :in-progress {},
   :done #{}})

(defn schedule
  "A map of tasks to their simulated start and end times, with which worker completed it. Can take a starting map or no starting map.
  Gives the remaining schedule based on the backlog, in-progress tasks, idle workers, and project clock. The in-progress tasks enjoy
  some clairvoyance since this is a simulated project."
  [project-time
   completed
   {:keys [backlog
           in-progress
           idle-workers]}]
  )

