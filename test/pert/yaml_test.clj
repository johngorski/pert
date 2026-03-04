(ns pert.yaml-test
  (:require
   [clj-yaml.core :as yaml]
   [clojure.test :refer :all]
   [pert.yaml :refer :all]
   ))



;; (yaml/parse-string bear-yaml :keywords false)
(def test-yaml
  (slurp "test/example.yml"))


(def test-yaml-data
  (yaml/parse-string test-yaml :keywords false))


(keys test-yaml-data)
;; => ("Breakdown" "Details")

(get test-yaml-data "Details")

(yml-data-breakdown test-yaml-data)
(yml-data-task-tree test-yaml-data)
(yml-data-parallel test-yaml-data)
(yml-data-details test-yaml-data)


(def loaded
  (backlog "test/example.yml"))

(map :estimate loaded)


(deftest parity)
(testing "YAML parity with")
(testing "scheduling namespace")
(testing "parsing extimates")
(is (=))
(into {}
      (comp
       (map (fn [{:keys [title estimate]}] [title estimate])))
      loaded)
;; => {"Cut cloth" [:pert-3pt 1.0 2.0 4.0], "Dress bear" [:pert-3pt 3.0 3.0 3.0], "Stuff fur" [:pert-3pt 4.0 6.0 13.0], "Embroider" [:pert-3pt 2.0 2.0 2.0], "Cut accessories" [:pert-3pt 0.0 1.0 3.0], "Package bear" [:pert-3pt 1.0 1.0 1.0], "Sew accessories" [:pert-3pt 0.0 2.0 3.0], "Sew clothes" [:pert-3pt 1.0 2.0 6.0], "Cut fur" [:pert-3pt 1.0 2.0 4.0], "Ship bear" [:pert-3pt 1.0 1.0 1.0]}
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

