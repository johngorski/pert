^{:nextjournal.clerk/visibility {:code :hide}}
(ns pert.notebooks.excel-demo
  {:nextjournal.clerk/visibility {:code :fold}}
  (:require
   [clojure.set :as sets]
   [nextjournal.clerk :as clerk]
   [hiccup2.core :as hiccup]
   [pert.excel :as excel]
   [pert.gantt :as gantt]
   [pert.graph :as graph]
   [pert.mermaid :as mermaid]
   [pert.report :as report]
   [pert.scheduling :as scheduling]
   [pert.task :as t]
   ))


^{::clerk/visibility {:code :hide}}
;; ^::clerk/no-cache
(let [_run-at #inst "2024-03-16T03:40:35.357-00:00"]
  (def example-file "test/example.xlsx"))

^{::clerk/visibility {:code :fold :result :hide}}
(def backlog (excel/backlog example-file))

^{::clerk/visibility {:code :hide :result :hide}}
(def date-fmt
  (java.text.SimpleDateFormat. "yyyy-MM-dd"))

^{::clerk/visibility {:code :hide :result :hide}}
(defn date-str [^java.util.Date d]
  (if d
    (.format date-fmt d)
    ""))

^{::clerk/viewer clerk/table
  ::clerk/visibility {:code :hide}}
(map (fn [task]
       (let [renamed (sets/rename-keys
                      task
                      {:id "ID"
                       :title "Title"
                       :started "Started"
                       :finished "Finished"
                       :description "Description"
                       :deps "Dependencies"
                       :estimate "Estimate"
                       })]
         (-> renamed
             (assoc "Title" (t/title-with-status task))
             (update "Started" date-str)
             (update "Finished" date-str)
             )))
     backlog)

^{::clerk/viewer (comp clerk/table clerk/use-headers)
  ::clerk/visibility {:code :hide}}
(report/status-report backlog)

^{::clerk/visibility {:code :hide :result :hide}}
(def mermaid-viewer
  ;; example from https://book.clerk.vision/
  {:transform-fn clerk/mark-presented
   :render-fn mermaid/render-fn})

^{::clerk/visibility {:code :hide :result :hide}}
(def dependency-mermaid (mermaid/graph (graph/simplified {:include-status? true} backlog)))

^{::clerk/visibility {:code :hide}}
(clerk/with-viewer mermaid-viewer dependency-mermaid)

;; Scheduling projection
^{::clerk/visibility {:code :hide :result :hide}}
(def n 1000)
^{::clerk/visibility {:code :hide}}
(str n " project task duration samples.")

^{::clerk/visibility {:code :hide :result :hide}}
;; ^::clerk/no-cache
;; (java.util.Date.)
(let [_run-at #inst "2024-03-15T06:49:40.223-00:00"]
  (def duration-samples
    (take n (scheduling/durations backlog))))

^{::clerk/visibility {:code :hide :result :hide}}
(defn team-of [n] (into #{} (range (if (<= 1.0 n) n 1))))

;; Schedule projection with one worker
^{::clerk/viewer clerk/html}
^::clerk/no-cache
(gantt/gantt-html {:cell-visual :bar} backlog duration-samples (team-of 1))

;; Schedule projection with two workers
^{::clerk/viewer clerk/html}
^::clerk/no-cache
(gantt/gantt-html {:cell-visual :bar} backlog duration-samples (team-of 2))

;; Schedule projection with three workers
^{::clerk/viewer clerk/html}
^::clerk/no-cache
(gantt/gantt-html {:cell-visual :bar} backlog duration-samples (team-of 3))

^{::clerk/visibility {:code :hide}
  ::clerk/viewer clerk/html}
[:pre dependency-mermaid]


(defn estimate-view [task]
  (let [{:keys [id title estimate description]} task
        [_ lo med high] estimate]
    {"ID" id
     "Title" title
     ;;  "Description" description
     "Low" lo
     "Estimate" med
     "High" high}))


;; estimates-table
(clerk/with-viewer
  clerk/table
  (map estimate-view backlog))


(def task-completion-day-data
  (gantt/gantt-data backlog duration-samples (team-of 1)))


(def finishing-threshold 0.97)


(defn simulations-finished [{:keys [finished] :as sims}]
  (let [total (apply + (vals sims))]
    (/ finished total)))


(defn day-finished [task-progress]
  (first ;; key of key/value pair
   (first ;; datum over threshold
    (sequence
     (comp
      (map simulations-finished)
      (map-indexed (fn [idx finished-ratio]
                     [idx finished-ratio]))
      (filter (fn [[_ ratio]] (< finishing-threshold ratio))))
     task-progress))))


(def task-completion-days
  (into {}
        (map (fn [[task-id sim-counts]]
               [task-id (day-finished sim-counts)]))
        task-completion-day-data))


;; Project day on which to expect task completion

(clerk/with-viewer
  clerk/table
  (let [start (scheduling/parse-date "2020-01-01")]
    (map (fn [task]
           (let [ete (get task-completion-days (:id task))
                 eta (scheduling/workday start ete)]
             {;; "ETE" (get task-completion-days (:id task))
              ;; "ID" (:id task)
              "Task" (t/title-with-status task)
              "ETA" (scheduling/yyyy-MM-dd eta)
              "Started" (or (some-> (:started task) scheduling/yyyy-MM-dd) "")
              "Finished" (or (some-> (:finished task) scheduling/yyyy-MM-dd) "")
              }))
         backlog)))
