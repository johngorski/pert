(ns pert.core-test
  (:require
   [clojure.test :refer :all]
   [pert.core :refer :all]))

(deftest estimate-shape
  (testing "Estimate bounds"
    (is (estimate? 1 2 3))
    (is (not (estimate? 2 1 4)))))
