(ns pert.mermaid
  (:require
   [clojure.string :as string]
   [nextjournal.clerk :as clerk]
   [pert.gantt :as gantt]
   [pert.random-variables :as random-variables]
   [pert.scheduling :as scheduling]
   ))

(defn mermaid-string [s] (str "\"" (string/escape s {\" "&quot;"}) "\""))
(defn mermaid-round [text] (str "(" (mermaid-string text) ")"))

(defn graph-syntax [graph-data]
  (let [[vertex-entries edges]
        (partition-by (comp map? second) graph-data)
        vertices (into {} vertex-entries)
        mermaid-lines (concat
                       (map (fn [[id {:keys [label]}]]
                              (str id (mermaid-round label)))
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

(def render-fn
  '(fn [value]
     (when value
       [nextjournal.clerk.render/with-d3-require
        {:package ["mermaid@8.14/dist/mermaid.js"]}
        (fn [mermaid]
          [:div {:ref (fn [el]
                        (when el
                          (.render mermaid
                                   (str (gensym))
                                   value
                                   #(set! (.-innerHTML el) %))))}])])))

