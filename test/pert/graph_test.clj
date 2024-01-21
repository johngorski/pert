(ns pert.graph-test
  (:require
   [clojure.test :refer :all]
   [pert.graph :refer :all]
   [pert.csv :as csv]
   [pert.scheduling :as scheduling]     ; tests for replacement method
                                        ; composition parity until
                                        ; they're removed
   ))

(deftest parity
  (testing "scheduling namepace parity"
    (testing "for vertices"
      (is (= '({:id "a", :label "Cut fur", :tooltip "Cut fur to the shape of the needed bear."}
               {:id "b", :label "Stuff fur",
                :tooltip "Stuff the cut fur to the right density for the quality of the product."}
               {:id "c", :label "Cut cloth", :tooltip "Cut cloth for the bear’s size"}
               {:id "d", :label "Sew clothes", :tooltip "Sew cloth into bear clothing"}
               {:id "e", :label "Embroider", :tooltip "Embroider custom name and message on the bear"}
               {:id "f", :label "Cut accessories", :tooltip "Cut ordered bear accessories"}
               {:id "g", :label "Sew accessories", :tooltip "Attach accessories to the bear"}
               {:id "h", :label "Dress bear", :tooltip "Dress bear in custom clothing"}
               {:id "i", :label "Package bear", :tooltip "Package bear for stackable shipping"}
               {:id "j", :label "Ship bear", :tooltip "Transport bear to shipping"})
             (:vertices (graph (csv/backlog "test/example.csv")))
             (map (comp vertex csv/task) (csv/rows "test/example.csv"))))
      )
    (testing "for edges"
      (is (= '#{["h" "b"] ["h" "e"] ["i" "h"] ["e" "d"] ["j" "i"]
                ["g" "f"] ["d" "c"] ["h" "g"] ["b" "a"]}
             (:edges (graph (csv/backlog "test/example.csv")))))
      )
    (testing "for full graph"
      (is (= #{["a" {:label "Cut fur", :tooltip "Cut fur to the shape of the needed bear."}]
               ["b" {:label "Stuff fur",
                     :tooltip "Stuff the cut fur to the right density for the quality of the product."}]
               ["c" {:label "Cut cloth", :tooltip "Cut cloth for the bear’s size"}]
               ["d" {:label "Sew clothes", :tooltip "Sew cloth into bear clothing"}]
               ["e" {:label "Embroider", :tooltip "Embroider custom name and message on the bear"}]
               ["f" {:label "Cut accessories", :tooltip "Cut ordered bear accessories"}]
               ["g" {:label "Sew accessories", :tooltip "Attach accessories to the bear"}]
               ["h" {:label "Dress bear", :tooltip "Dress bear in custom clothing"}]
               ["i" {:label "Package bear", :tooltip "Package bear for stackable shipping"}]
               ["j" {:label "Ship bear", :tooltip "Transport bear to shipping"}]
               ["h" "b"] ["h" "e"] ["i" "h"] ["e" "d"] ["j" "i"]
               ["g" "f"] ["d" "c"] ["h" "g"] ["b" "a"]}
             (let [{:keys [vertices edges]} (graph (map csv/task (csv/rows "test/example.csv")))]
               (into edges
                     (map (fn [v] [(:id v) (dissoc v :id)]))
                     vertices))))
      )
    ))

