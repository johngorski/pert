^{:nextjournal.clerk/visibility {:code :hide}}
(ns pert.notebooks.demo
  {:nextjournal.clerk/visibility {:code :fold}}
  (:require
   [clojure.set :as sets]
   [nextjournal.clerk :as clerk]
   [hiccup2.core :as hiccup]
   [pert.csv :as csv]
   [pert.gantt :as gantt]
   [pert.graph :as graph]
   [pert.mermaid :as mermaid]
   [pert.scheduling :as scheduling]
   [pert.task :as t]
   ))

^{::clerk/visibility {:code :hide}}
;; ^::clerk/no-cache
#_(java.util.Date.)
(let [_run-at #inst "2024-03-16T03:40:35.357-00:00"]
  (def example-file "test/example.csv"))
  ;; (def example-file "test/overcooked-burger.csv"))

^{::clerk/visibility {:code :fold :result :hide}}
(def backlog (csv/backlog example-file))

^{::clerk/viewer clerk/table
  ::clerk/visibility {:code :hide}}
(map (fn [task]
       (let [renamed (sets/rename-keys
                      task
                      {:id "ID"
                       :title "Title"
                       :description "Description"
                       :deps "Dependencies"
                       :estimate "Estimate"
                       })]
         (assoc renamed "Title" (t/title-with-status task))
         ))
     backlog)

^{::clerk/visibility {:code :hide :result :hide}}
(def mermaid-viewer
  ;; example from https://book.clerk.vision/
  {:transform-fn clerk/mark-presented
   :render-fn mermaid/render-fn})

^{::clerk/visibility {:code :hide :result :hide}}
(def dependency-mermaid (mermaid/graph (graph/simplified backlog)))

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
(gantt/gantt-html {:cell-visual :gradient} backlog duration-samples (team-of 2))

;; Schedule projection with three workers
^{::clerk/viewer clerk/html}
^::clerk/no-cache
(gantt/gantt-html {:cell-visual :gradient} backlog duration-samples (team-of 3))

^{::clerk/visibility {:code :hide}
  ::clerk/viewer clerk/html}
[:pre dependency-mermaid]
