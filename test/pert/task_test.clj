(ns pert.task-test
  (:require
   [clojure.test :refer :all]
   [pert.task :refer :all]
   ))


(deftest status-examples
  (testing "task status"
    (testing "examples"
      (is (= :done (status {:finished :yes})))
      (is (= :in-progress (status {:started :yes})))
      (is (= :to-do (status {}))))))
