(ns pert.core-test
  (:require
   [clojure.test :refer :all]
   [pert.core :refer :all]
   [pert.csv :as csv]
   [pert.graph :as graph]
   [pert.mermaid :as mermaid]
   [pert.scheduling :as scheduling]
   ))

(deftest estimate-shape
  (testing "Estimate bounds"
    (is (estimate? 1 2 3))
    (is (not (estimate? 2 1 4)))))

