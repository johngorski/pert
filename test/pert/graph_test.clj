(ns pert.graph-test
  (:require
   [clojure.test :refer :all]
   [pert.graph :refer :all]
   [pert.csv :as csv]
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
             (:edges (simplified (csv/backlog "test/example.csv")))))
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
             (let [{:keys [vertices edges]} (simplified (map csv/task (csv/rows "test/example.csv")))]
               (into edges
                     (map (fn [v] [(:id v) (dissoc v :id)]))
                     vertices))))
      )
    ))

(deftest algos
  (testing "topological sort"
    (testing "is empty for empty graphs"
      (is (= [] (sort-topological {}))))
    (testing "throws on cyclic graphs"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Cycle detected"
                            (sort-topological {:a #{:b} :b #{:a}})
                            )))
    (testing "does fine with a single-node graph"
      (is (= [:a] (sort-topological {:a #{}}))))
    (testing "does fine with a single-edge graph"
      (is (= [:b :a] (sort-topological {:a #{:b} :b #{}}))))
    (testing "follows a simple chain of dependencies"
      (is (= [:d :c :a :b] (sort-topological {:a #{:c} :b #{:a} :c #{:d} :d #{}}))))
    (testing "can sort a redundant set of dependencies"
      (is (= [:e :d :b :c :a]
             (sort-topological {:a #{:b :c :d :e}
                                :b #{:d}
                                :c #{:d :e}
                                :d #{:e}
                                :e #{}
                                }))))
    )
  (testing "construction"
    (testing "by with-edge"
      (testing "includes the target vertex in graph keys when it has not yet been seen"
        (is (= {:a #{:b} :b #{}}
               (with-edge {} :a :b))))
      ))
  (testing "transitive reduction"
    (testing "matches example at https://brunoscheufler.com/blog/2021-12-05-decreasing-graph-complexity-with-transitive-reductions"
      (is (= {:d #{:e}, :b #{:d}, :c #{:d}, :a #{:c :b}, :e #{}}
             (transitive-reduction {:a #{:b :c :d :e}
                                    :b #{:d}
                                    :c #{:d :e}
                                    :d #{:e}
                                    :e #{}
                                    }))))
    (testing "leaves reduced graphs undisturbed"
      (let [reduced {:a #{:b :c}, :b #{}, :c #{:d}, :d #{}}]
        (is (= reduced (transitive-reduction reduced))))))
  (testing "simplification"
    (testing "honors transitive reduction"
      (is (= {:vertices [:a :b :c :d]
              :edges #{[:c :d] [:a :b] [:b :d] [:a :c]}}
             (-> (simplified [{:id :a :deps #{:b :c :d}}
                              {:id :b :deps #{:d}}
                              {:id :c :deps #{:d}}
                              {:id :d :deps #{}}
                              ])
                 (update :vertices #(map :id %))))))))
