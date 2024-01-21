(ns pert.graphviz
  (:require
   [dorothy.core :as dorothy]))

(defn vertex
  "v is a graph/vertex (TODO: spec)"
  [v]
  [(:id v) (dissoc v :id)])

;; TODO: Try this out.
(defn dot
  "graph is a graph/graph (TODO: spec)"
  [{:keys [vertices edges]}]
  (dorothy/dot
   (dorothy/digraph
    (concat
     (map vertex vertices)
     edges
     ))))
