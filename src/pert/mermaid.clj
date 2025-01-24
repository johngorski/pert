(ns pert.mermaid
  (:require
   [clojure.string :as string]
   ))

(defn mermaid-string [s] (str "\"" (string/escape s {\" "&quot;"}) "\""))
(defn mermaid-round [text] (str "(" (mermaid-string text) ")"))

(defn mermaid-graph-direction [from to]
  (let [default "RL"
        ;; turns out there aren't any diagonal graph directions
        dir-representations {:top "TB" ;; to bottom
                             :bottom "BT" ;; to top
                             :left "LR" ;; to right
                             :right "RL" ;; to left
                             }]
    (get dir-representations from default)))

(defn graph
  "Mermaid graph syntax for graph defined on tasks from pert.graph namespace."
  ([g] (graph {:direction [:right :left]} g))
  ([props graph]
   (let [{:keys [direction]} props
         dir (apply mermaid-graph-direction direction)
         {:keys [vertices edges]} graph
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
     (string/join "\n" (concat [(str "graph " dir)] (map (fn [line] (str "    " line)) mermaid-lines)) )
     )))

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

