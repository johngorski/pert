^{:nextjournal.clerk/visibility {:code :hide}}
(ns pert.notebooks.demo
  {:nextjournal.clerk/visibility {:code :fold}}
  (:require
   [nextjournal.clerk :as clerk]
   [pert.gantt :as gantt]
   [pert.mermaid :as mermaid]
   [pert.random-variables :as random-variables]
   [pert.scheduling :as scheduling]
   ))

^{::clerk/visibility {:code :hide}}
;; ^::clerk/no-cache
(def example-file "test/example.csv")

^{::clerk/viewer clerk/table
  ::clerk/visibility {:code :hide}}
(scheduling/csv->rows example-file)

^{::clerk/visibility {:code :hide :result :hide}}
(def mermaid-viewer
  ;; example from https://book.clerk.vision/
  {:transform-fn clerk/mark-presented
   :render-fn mermaid/render-fn})

^{::clerk/visibility {:code :hide :result :hide}}
(def dependency-mermaid (mermaid/graph-syntax (scheduling/csv->graph-data example-file)))

^{::clerk/visibility {:code :hide}}
(clerk/with-viewer mermaid-viewer dependency-mermaid)

;; Schedule projection with one worker
^{::clerk/viewer clerk/html}
(gantt/csv->gantt-html 1 example-file)

;; Schedule projection with two workers
^{::clerk/viewer clerk/html}
(gantt/csv->gantt-html 2 example-file)

;; Schedule projection with three workers
^{::clerk/viewer clerk/html}
(gantt/csv->gantt-html 3 example-file)

^{::clerk/visibility {:code :hide}
  ::clerk/viewer clerk/html}
[:pre dependency-mermaid]
