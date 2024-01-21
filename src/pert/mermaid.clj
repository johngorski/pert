(ns pert.mermaid
  (:require
   [clojure.string :as string]
   ))

(defn mermaid-string [s] (str "\"" (string/escape s {\" "&quot;"}) "\""))
(defn mermaid-round [text] (str "(" (mermaid-string text) ")"))

(defn graph
  "Mermaid graph syntax for graph defined on tasks from pert.graph namespace."
  [graph]
  (let [{:keys [vertices edges]} graph
        vertex-lookup (into {} (map (fn [v] [(:id v) (dissoc v :id)])) vertices)
        mermaid-lines (concat
                       (map (fn [[id {:keys [label]}]]
                              (str id (mermaid-round label)))
                            vertex-lookup)
                       (map (fn [[from to]]
                              (str
                               from
                               (mermaid-round (get-in vertex-lookup [from :label]))
                               " --> "
                               to
                               (mermaid-round (get-in vertex-lookup [to :label]))
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

