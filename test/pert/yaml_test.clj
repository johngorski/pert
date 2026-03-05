(ns pert.yaml-test
  (:require
   [clj-yaml.core :as yaml]
   [clojure.set :as set]
   [clojure.test :refer :all]
   [pert.yaml :refer :all]
   ))


(defn- deps-by-title
  "Translate a backlog's numeric-ID deps into title-based deps for stable assertions."
  [b]
  (let [id->title (into {} (map (fn [{:keys [id title]}] [id title])) b)]
    (into {}
          (map (fn [{:keys [title deps]}]
                 [title (set (map id->title deps))]))
          b)))


(defn- estimates-by-title
  "Map from task title to estimate vector."
  [b]
  (into {}
        (map (fn [{:keys [title estimate]}] [title estimate]))
        b))


(deftest parity
  (testing "YAML parity with"
    (testing "scheduling namespace"
      (let [b (backlog "test/example.yml")]
        (testing "parsing estimates"
          (is (= (estimates-by-title b)
                 {"Cut fur"        [:pert-3pt 1.0 2.0 4.0]
                  "Stuff fur"      [:pert-3pt 4.0 6.0 13.0]
                  "Cut cloth"      [:pert-3pt 1.0 2.0 4.0]
                  "Sew clothes"    [:pert-3pt 1.0 2.0 6.0]
                  "Embroider"      [:pert-3pt 2.0 2.0 2.0]
                  "Cut accessories" [:pert-3pt 0.0 1.0 3.0]
                  "Sew accessories" [:pert-3pt 0.0 2.0 3.0]
                  "Dress bear"     [:pert-3pt 3.0 3.0 3.0]
                  "Package bear"   [:pert-3pt 1.0 1.0 1.0]
                  "Ship bear"      [:pert-3pt 1.0 1.0 1.0]})))
        (testing "parsing backlog dependencies"
          (is (= (deps-by-title b)
                 {"Cut fur"        #{}
                  "Stuff fur"      #{"Cut fur"}
                  "Cut cloth"      #{}
                  "Sew clothes"    #{"Cut cloth"}
                  "Embroider"      #{"Cut cloth" "Sew clothes"}
                  "Cut accessories" #{}
                  "Sew accessories" #{"Cut accessories"}
                  "Dress bear"     #{"Stuff fur" "Embroider" "Sew accessories"}
                  "Package bear"   #{"Dress bear"}
                  "Ship bear"      #{"Dress bear" "Package bear"}})))
        (testing "3 point pert estimates"
          (is (= (estimates-by-title b)
                 (estimates-by-title (backlog "test/example.yml")))))
        ))))


(deftest example
  (testing "example project backlog"
    (let [b (backlog "test/example.yml")]
      (testing "has the correct number of tasks"
        (is (= 10 (count b))))
      (testing "all tasks have required keys"
        (doseq [task b]
          (is (contains? task :id))
          (is (contains? task :title))
          (is (contains? task :deps))
          (is (contains? task :estimate))))
      (testing "all tasks have descriptions"
        (is (= (set (map :title (filter :description b)))
               #{"Cut fur" "Stuff fur" "Cut cloth" "Sew clothes"
                 "Embroider" "Cut accessories" "Sew accessories"
                 "Dress bear" "Package bear" "Ship bear"})))
      (testing "descriptions match"
        (is (= (into {} (map (fn [{:keys [title description]}] [title description])) b)
               {"Cut fur"        "Cut fur to the shape of the needed bear."
                "Stuff fur"      "Stuff the cut fur to the right density for the quality of the product."
                "Cut cloth"      "Cut cloth for the bear's size"
                "Sew clothes"    "Sew cloth into bear clothing"
                "Embroider"      "Embroider custom name and message on the bear"
                "Cut accessories" "Cut ordered bear accessories"
                "Sew accessories" "Attach accessories to the bear"
                "Dress bear"     "Dress bear in custom clothing"
                "Package bear"   "Package bear for stackable shipping"
                "Ship bear"      "Transport bear to shipping"})))
      (testing "started and finished dates"
        (let [by-title (into {} (map (fn [t] [(:title t) t])) b)]
          (is (= "1/1/2020" (:started (by-title "Cut fur"))))
          (is (= "1/3/2020" (:finished (by-title "Cut fur"))))
          (is (= "1/3/2020" (:started (by-title "Stuff fur"))))
          (is (= "1/14/2020" (:finished (by-title "Stuff fur"))))
          (is (= "1/14/2020" (:started (by-title "Cut cloth"))))
          (is (nil? (:started (by-title "Dress bear"))))
          (is (nil? (:finished (by-title "Dress bear"))))))
      (testing "IDs are sequential numeric strings"
        (is (= (set (map :id b))
               (set (map str (range 1 11))))))
      (testing "tasks are sorted by numeric ID"
        (is (= (map :id b)
               (sort-by #(Integer/parseInt %) (map :id b))))))))


(def dep-yaml-parsed
  (get (yaml/parse-string "
Tasks:
- '1':
  - '1.1'
  - '1.2'
- '2':
  - '2.1'
  - '2.2':
    - '2.2.1'
    - '2.2.2'
  - '2.3'
- '3':
  - '3.1'
- '4':
  - '4.1'
  - '4.2'
  - '4.3'
" :keywords false) "Tasks"))


(deftest dependency-generation
  (testing "dep-layer extracts top-level task names"
    (is (= '("1" "2" "3" "4")
           (dep-layer dep-yaml-parsed))))

  (testing "dep-layers returns preorder traversal of layers"
    (is (= '(("1" "2" "3" "4")
             ("1.1" "1.2")
             ("2.1" "2.2" "2.3")
             ("2.2.1" "2.2.2")
             ("3.1")
             ("4.1" "4.2" "4.3"))
           (dep-layers dep-yaml-parsed))))

  (testing "prev-deps: later tasks depend on previous at same layer"
    (is (= {"1" #{}
            "1.1" #{}
            "1.2" #{"1.1"}
            "2" #{"1"}
            "2.1" #{}
            "2.2" #{"2.1"}
            "2.2.1" #{}
            "2.2.2" #{"2.2.1"}
            "2.3" #{"2.1" "2.2"}
            "3" #{"1" "2"}
            "3.1" #{}
            "4" #{"1" "2" "3"}
            "4.1" #{}
            "4.2" #{"4.1"}
            "4.3" #{"4.1" "4.2"}}
           (prev-deps dep-yaml-parsed))))

  (testing "child-deps: parent tasks depend on their subtasks"
    (is (= {"1" #{"1.1" "1.2"}
            "2" #{"2.1" "2.2" "2.3"}
            "2.2" #{"2.2.1" "2.2.2"}
            "3" #{"3.1"}
            "4" #{"4.1" "4.2" "4.3"}}
           (child-deps dep-yaml-parsed))))

  (testing "parent-tasks: subtask to parent mapping"
    (is (= {"1.1" "1"
            "1.2" "1"
            "2.1" "2"
            "2.2" "2"
            "2.3" "2"
            "2.2.1" "2.2"
            "2.2.2" "2.2"
            "3.1" "3"
            "4.1" "4"
            "4.2" "4"
            "4.3" "4"}
           (parent-tasks dep-yaml-parsed))))

  (testing "parent-deps: children inherit parent's prev-deps"
    (is (= {"1.1" #{}
            "1.2" #{}
            "2.1" #{"1"}
            "2.2" #{"1"}
            "2.2.1" #{"2.1"}
            "2.2.2" #{"2.1"}
            "2.3" #{"1"}
            "3.1" #{"1" "2"}
            "4.1" #{"1" "2" "3"}
            "4.2" #{"1" "2" "3"}
            "4.3" #{"1" "2" "3"}}
           (parent-deps dep-yaml-parsed))))

  (testing "breakdown-deps: combined prev + child deps"
    (is (= {"1" #{"1.1" "1.2"}
            "1.1" #{}
            "1.2" #{"1.1"}
            "2" #{"1" "2.1" "2.2" "2.3"}
            "2.1" #{}
            "2.2" #{"2.1" "2.2.1" "2.2.2"}
            "2.2.1" #{}
            "2.2.2" #{"2.2.1"}
            "2.3" #{"2.1" "2.2"}
            "3" #{"1" "2" "3.1"}
            "3.1" #{}
            "4" #{"1" "2" "3" "4.1" "4.2" "4.3"}
            "4.1" #{}
            "4.2" #{"4.1"}
            "4.3" #{"4.1" "4.2"}}
           (breakdown-deps dep-yaml-parsed)))))


(deftest parallel-tasks
  (testing "remove-parallel-deps strips cross-deps between parallel tasks"
    (let [parallel #{"A" "B" "C"}
          deps {"A" #{"X" "B"}
                "B" #{"X" "A" "C"}
                "C" #{"Y"}
                "D" #{"A" "B" "C"}}]
      (is (= {"A" #{"X"}
              "B" #{"X"}
              "C" #{"Y"}
              "D" #{"A" "B" "C"}}
             (remove-parallel-deps parallel deps))))))


(deftest estimate-parsing
  (testing "3-point estimate"
    (is (= [:pert-3pt 1.0 2.0 4.0]
           (estimate {"Type" "3-point" "Low" 1 "Estimate" 2 "High" 4}))))
  (testing "gaussian estimate"
    (is (= [:gaussian 5.0 1.5]
           (estimate {"Type" "gaussian" "Estimate" 5 "StandardVariation" 1.5}))))
  (testing "certain estimate"
    (is (= [:definitely 3.0]
           (estimate {"Type" "certain" "Estimate" 3}))))
  (testing "even estimate"
    (is (= [:uniform 1.0 5.0]
           (estimate {"Type" "even" "Low" 1 "High" 5}))))
  (testing "3-point gaussian estimate"
    (is (= [:pert 2.0 3.0 5.0]
           (estimate {"Type" "3-point gaussian" "Low" 2 "Estimate" 3 "High" 5})))))


(deftest validation
  (testing "Low <= Estimate <= High")
  )
