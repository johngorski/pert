(ns pert.csv-test
  (:require
   [clojure.test :refer :all]
   [pert.csv :refer :all]
   ))


(deftest parity
  (testing "CSV parity with"
    (testing "scheduling namespace"
      (testing "parsing estimates"
        (is (= (into {}
                     (comp
                      (map task)
                      (map (fn [{:keys [id estimate]}] [id estimate])))
                     (rows "test/example.csv"))
               {"d" [:pert-3pt 1.0 2.0 6.0]
                "f" [:pert-3pt 0.0 1.0 3.0]
                "e" [:pert-3pt 2.0 2.0 2.0]
                "j" [:pert-3pt 1.0 1.0 1.0]
                "a" [:pert-3pt 1.0 2.0 4.0]
                "i" [:pert-3pt 1.0 1.0 1.0]
                "b" [:pert-3pt 4.0 6.0 13.0]
                "g" [:pert-3pt 0.0 2.0 3.0]
                "h" [:pert-3pt 3.0 3.0 3.0]
                "c" [:pert-3pt 1.0 2.0 4.0]}
               )))
      (testing "parsing backlog"
        (is (= '({:id "a", :deps #{}}
                 {:id "b", :deps #{"a"}}
                 {:id "c", :deps #{}}
                 {:id "d", :deps #{"c"}}
                 {:id "e", :deps #{"d"}}
                 {:id "f", :deps #{}}
                 {:id "g", :deps #{"f"}}
                 {:id "h", :deps #{"e" "b" "g"}}
                 {:id "i", :deps #{"h"}}
                 {:id "j", :deps #{"i" "e"}})
               (sequence
                (comp
                 (map task)
                 (map #(select-keys % [:id :deps]))
                 )
                (rows "test/example.csv"))
               (map #(select-keys % [:id :deps]) (backlog "test/example.csv"))
               )))
      (testing "3 point pert estimates"
        (is (= (into {}
                     (map (fn [{:keys [id estimate]}]
                            [id estimate]))
                     (backlog "test/example.csv"))
               {"d" [:pert-3pt 1.0 2.0 6.0]
                "f" [:pert-3pt 0.0 1.0 3.0]
                "e" [:pert-3pt 2.0 2.0 2.0]
                "j" [:pert-3pt 1.0 1.0 1.0]
                "a" [:pert-3pt 1.0 2.0 4.0]
                "i" [:pert-3pt 1.0 1.0 1.0]
                "b" [:pert-3pt 4.0 6.0 13.0]
                "g" [:pert-3pt 0.0 2.0 3.0]
                "h" [:pert-3pt 3.0 3.0 3.0]
                "c" [:pert-3pt 1.0 2.0 4.0]}
               )))
      )))
(into {}
      (map (fn [{:keys [id estimate]}]
             [id estimate]))
      (backlog "test/example.csv"))


(deftest csv-parsing
  (testing "Dependency cells split as expected"
    (is (= #{"a" "b" "c" "d" "ef" "g"}) (parse-dependencies ",,a,b  ,c ,   d, ef, g,,,,")))
  (testing "Empty dependency string means no dependencies"
    (is (= #{} (parse-dependencies "")))
    (is (= #{} (parse-dependencies " \t  ")))
    (is (= #{} (parse-dependencies nil))))
  (testing "rows without IDs are comments"
    (is (= (count (backlog "test/comment-row.csv"))
           1))))

(backlog "test/example.csv")



(deftest example
  (testing "example project backlog"
    (is (= (backlog "test/example.csv")
           '({:id "a", :title "Cut fur", :description "Cut fur to the shape of the needed bear.", :deps #{}, :started "1/1/2020", :finished "1/3/2020", :estimate [:pert-3pt 1.0 2.0 4.0]}
             {:id "b", :title "Stuff fur", :description "Stuff the cut fur to the right density for the quality of the product.", :deps #{"a"}, :started "1/3/2020", :finished "1/14/2020", :estimate [:pert-3pt 4.0 6.0 13.0]}
             {:id "c", :title "Cut cloth", :description "Cut cloth for the bear’s size", :deps #{}, :started "1/14/2020", :estimate [:pert-3pt 1.0 2.0 4.0]}
             {:id "d", :title "Sew clothes", :description "Sew cloth into bear clothing", :deps #{"c"}, :estimate [:pert-3pt 1.0 2.0 6.0]}
             {:id "e", :title "Embroider", :description "Embroider custom name and message on the bear", :deps #{"d"}, :estimate [:pert-3pt 2.0 2.0 2.0]}
             {:id "f", :title "Cut accessories", :description "Cut ordered bear accessories", :deps #{}, :estimate [:pert-3pt 0.0 1.0 3.0]}
             {:id "g", :title "Sew accessories", :description "Attach accessories to the bear", :deps #{"f"}, :estimate [:pert-3pt 0.0 2.0 3.0]}
             {:id "h", :title "Dress bear", :description "Dress bear in custom clothing", :deps #{"e" "b" "g"}, :estimate [:pert-3pt 3.0 3.0 3.0]}
             {:id "i", :title "Package bear", :description "Package bear for stackable shipping", :deps #{"h"}, :estimate [:pert-3pt 1.0 1.0 1.0]}
             {:id "j", :title "Ship bear", :description "Transport bear to shipping", :deps #{"e" "i"}, :estimate [:pert-3pt 1.0 1.0 1.0]})
           ))))



(deftest validation
  (testing "Low <= Estimate <= High")
  )

