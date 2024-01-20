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

^::clerk/no-cache
^{::clerk/visibility {:code :hide}}
(def example-file "test/example.csv")

^{::clerk/viewer clerk/table
  ::clerk/visibility {:code :hide}}
(def rows
  (scheduling/csv->rows example-file))

^{::clerk/visibility {:code :hide :result :hide}}
(defn mermaid-string [s] (str "\"" (string/escape s {\" "&quot;"}) "\""))
^{::clerk/visibility {:code :hide :result :hide}}
(defn mermaid-round [text] (str "(" (mermaid-string text) ")"))

^{::clerk/visibility {:code :hide :result :hide}}
(defn graph-data->mermaid [graph-data]
  (let [[vertex-entries edges]
        (partition-by (comp map? second) graph-data)
        vertices (into {} vertex-entries)
        mermaid-lines (concat
                       (map (fn [[id {:keys [label]}]]
                              (str id "(\"" label "\")"))
                            vertex-entries)
                       (map (fn [[from to]]
                              (str
                               from
                               (mermaid-round (get-in vertices [from :label]))
                               " --> "
                               to
                               (mermaid-round (get-in vertices [to :label]))
                               )) edges))
        ]
    (string/join "\n" (concat ["graph RL"] (map (fn [line] (str "    " line)) mermaid-lines)) )
    ))

^{::clerk/visibility {:code :hide :result :hide}}
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

^{::clerk/visibility {:code :hide :result :hide}}
(def dependency-mermaid (graph-data->mermaid (scheduling/csv->graph-data example-file)))

^{::clerk/visibility {:code :hide}}
(clerk/with-viewer mermaid-viewer dependency-mermaid)

;; Schedule projection with one worker

^{::clerk/viewer clerk/html}
(gantt/csv->gantt-html 1 example-file)

;; Schedule projection with two workers

^{::clerk/viewer clerk/html}
(gantt/csv->gantt-html 2, example-file)

;; Schedule projection with three workers

^{::clerk/viewer clerk/html}
(gantt/csv->gantt-html 3 example-file)

^{::clerk/visibility {:code :hide}
  ::clerk/viewer clerk/html}
[:pre dependency-mermaid]
