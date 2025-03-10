(ns pert.excel-test
  (:require
   [clojure.test :refer :all]
   [pert.excel :refer :all]
   [pert.spreadsheet :as spreadsheet]
   ))


(deftest parity
  (testing "Excel parity with"
    (testing "scheduling namespace"
      (testing "parsing estimates"
        (is (= (into {}
                     (comp
                      (map spreadsheet/task)
                      (map (fn [{:keys [id estimate]}] [id estimate])))
                     (rows "test/example.xlsx"))
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
                 (map spreadsheet/task)
                 (map #(select-keys % [:id :deps]))
                 )
                (rows "test/example.xlsx"))
               (map #(select-keys % [:id :deps]) (backlog "test/example.xlsx"))
               )))
      (testing "3 point pert estimates"
        (is (= (into {}
                     (map (fn [{:keys [id estimate]}]
                            [id estimate]))
                     (backlog "test/example.xlsx"))
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
      (backlog "test/example.xlsx"))


(deftest excel-parsing
  (testing "Dependency cells split as expected"
    (is (= #{"a" "b" "c" "d" "ef" "g"}) (spreadsheet/parse-dependencies ",,a,b  ,c ,   d, ef, g,,,,")))
  (testing "Empty dependency string means no dependencies"
    (is (= #{} (spreadsheet/parse-dependencies "")))
    (is (= #{} (spreadsheet/parse-dependencies " \t  ")))
    (is (= #{} (spreadsheet/parse-dependencies nil))))
  (testing "rows without IDs are comments"
    (is (= (count (backlog "test/comment-row.xlsx"))
           1))))

(deftest example
  (testing "example project backlog"
    (is (= (backlog "test/example.xlsx")
           '({:id "a", :title "Cut fur", :description "Cut fur to the shape of the needed bear."
              :deps #{}
              :started #inst "2020-01-01T08:00:00.000-00:00"
              :finished #inst "2020-01-03T08:00:00.000-00:00"
              :estimate [:pert-3pt 1.0 2.0 4.0]}
             {:id "b", :title "Stuff fur", :description "Stuff the cut fur to the right density for the quality of the product.", :deps #{"a"}, :started #inst "2020-01-03T08:00:00.000-00:00", :finished #inst "2020-01-14T08:00:00.000-00:00", :estimate [:pert-3pt 4.0 6.0 13.0]}
             {:id "c", :title "Cut cloth", :description "Cut cloth for the bear‚Äôs size", :deps #{}, :started #inst "2020-01-14T08:00:00.000-00:00", :finished nil, :estimate [:pert-3pt 1.0 2.0 4.0]}
             {:id "d", :title "Sew clothes", :description "Sew cloth into bear clothing", :deps #{"c"}, :started nil, :finished nil, :estimate [:pert-3pt 1.0 2.0 6.0]}
             {:id "e", :title "Embroider", :description "Embroider custom name and message on the bear", :deps #{"d"}, :started nil, :finished nil, :estimate [:pert-3pt 2.0 2.0 2.0]}
             {:id "f", :title "Cut accessories", :description "Cut ordered bear accessories", :deps #{}, :started nil, :finished nil, :estimate [:pert-3pt 0.0 1.0 3.0]}
             {:id "g", :title "Sew accessories", :description "Attach accessories to the bear", :deps #{"f"}, :started nil, :finished nil, :estimate [:pert-3pt 0.0 2.0 3.0]}
             {:id "h", :title "Dress bear", :description "Dress bear in custom clothing", :deps #{"e" "b" "g"}, :started nil, :finished nil, :estimate [:pert-3pt 3.0 3.0 3.0]}
             {:id "i", :title "Package bear", :description "Package bear for stackable shipping", :deps #{"h"}, :started nil, :finished nil, :estimate [:pert-3pt 1.0 1.0 1.0]}
             {:id "j", :title "Ship bear", :description "Transport bear to shipping", :deps #{"e" "i"}, :started nil, :finished nil, :estimate [:pert-3pt 1.0 1.0 1.0]})
           ))))



(deftest validation
  (testing "Low <= Estimate <= High")
  )



