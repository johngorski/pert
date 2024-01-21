(ns pert.mermaid-test
  (:require
   [clojure.test :refer :all]
   [pert.csv :as csv]
   [pert.graph :as graph]
   [pert.mermaid :as mermaid]))

(deftest example
  (testing "example file dependencies"
    (is (= (mermaid/graph (graph/graph (csv/backlog "test/example.csv")))
           "graph RL
    d(\"Sew clothes\")
    f(\"Cut accessories\")
    e(\"Embroider\")
    j(\"Ship bear\")
    a(\"Cut fur\")
    i(\"Package bear\")
    b(\"Stuff fur\")
    g(\"Sew accessories\")
    h(\"Dress bear\")
    c(\"Cut cloth\")
    h(\"Dress bear\") --> b(\"Stuff fur\")
    h(\"Dress bear\") --> e(\"Embroider\")
    i(\"Package bear\") --> h(\"Dress bear\")
    e(\"Embroider\") --> d(\"Sew clothes\")
    j(\"Ship bear\") --> i(\"Package bear\")
    g(\"Sew accessories\") --> f(\"Cut accessories\")
    d(\"Sew clothes\") --> c(\"Cut cloth\")
    h(\"Dress bear\") --> g(\"Sew accessories\")
    b(\"Stuff fur\") --> a(\"Cut fur\")"))))
