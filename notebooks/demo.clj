^{:nextjournal.clerk/visibility {:code :hide}}
(ns pert.notebooks.demo
  {:nextjournal.clerk/visibility {:code :fold}}
  (:require
   [clojure.string :as string]
   [nextjournal.clerk :as clerk]
   [pert.gantt :as gantt]
   [pert.random-variables :as random-variables]
   [pert.scheduling :as scheduling]
   ))

(def example-file "test/example.csv")

^{::clerk/viewer clerk/table}
(def rows
  (scheduling/csv->rows example-file))

(defn graph-data->mermaid [graph-data]
  (let [[vertex-entries edges]
        (partition-by (comp map? second) graph-data)

        vertices (into {} vertex-entries)
        mermaid-lines (map (fn [[from to]]
                             (str
                              from
                              "("
                              (get-in vertices [from :label])
                              ") --> "
                              to
                              "("
                              (get-in vertices [to :label])
                              ")"
                              )) edges)
        ]
    (string/join "\n" (concat ["graph RL"] (map (fn [line] (str "    " line)) mermaid-lines)) )
    ))

(def mermaid-viewer
  ;; example from https://book.clerk.vision/
  {:transform-fn clerk/mark-presented
   :render-fn '(fn [value]
                 (when value
                   [nextjournal.clerk.render/with-d3-require
                    {:package ["mermaid@8.14/dist/mermaid.js"]}
                    (fn [mermaid]
                      [:div {:ref (fn [el]
                                    (when el
                                      (.render mermaid
                                               (str (gensym))
                                               value
                                               #(set! (.-innerHTML el) %))))}])]))})

(clerk/with-viewer mermaid-viewer
  (graph-data->mermaid (scheduling/csv->graph-data example-file)))

^{::clerk/viewer clerk/html}
(gantt/csv->gantt-html example-file)
