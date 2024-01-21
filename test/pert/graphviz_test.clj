(ns pert.graphviz-test
  (:require
   [clojure.test :refer :all]
   [pert.graphviz :refer :all]
   [pert.graph :as graph]
   [pert.csv :as csv]
   [pert.scheduling :as scheduling]     ; tests for replacement method
                                        ; composition parity until
                                        ; they're removed
   ))

(deftest example
  (testing "example dependency graphviz"
    (is (= (dot (graph/graph (map csv/task (csv/rows "test/example.csv"))))
           "digraph {
a [label=\"Cut fur\",tooltip=\"Cut fur to the shape of the needed bear.\"];
b [label=\"Stuff fur\",tooltip=\"Stuff the cut fur to the right density for the quality of the product.\"];
c [label=\"Cut cloth\",tooltip=\"Cut cloth for the bear’s size\"];
d [label=\"Sew clothes\",tooltip=\"Sew cloth into bear clothing\"];
e [label=Embroider,tooltip=\"Embroider custom name and message on the bear\"];
f [label=\"Cut accessories\",tooltip=\"Cut ordered bear accessories\"];
g [label=\"Sew accessories\",tooltip=\"Attach accessories to the bear\"];
h [label=\"Dress bear\",tooltip=\"Dress bear in custom clothing\"];
i [label=\"Package bear\",tooltip=\"Package bear for stackable shipping\"];
j [label=\"Ship bear\",tooltip=\"Transport bear to shipping\"];
h -> b;
h -> e;
i -> h;
e -> d;
j -> i;
g -> f;
d -> c;
h -> g;
b -> a;
} "))))

