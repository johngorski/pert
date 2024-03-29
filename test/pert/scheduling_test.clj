(ns pert.scheduling-test
  (:require
   [clojure.java.io :as io]
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [clojure.test :refer :all]
   [hiccup2.core :as hiccup]
   [pert.csv :as csv]
   [pert.random-variables :as random-variables]
   [pert.scheduling :refer :all]))

(def d 0.001)

(defn =?
  "Are the arguments within the given tolerance?"
  [tolerance & args]
  (<= (- (apply max args) (apply min args)) tolerance))

(deftest project-records
  (testing "Record for job with 0 tasks"
    (is (= (project-record {})
           {})))
  (testing "Record for a job with 1 task"
    (let [t "Task 1"
          lo 1
          nom 2
          hi 5
          record (project-record {:backlog [{:id t}]
                                  :estimates {t [:pert-3pt lo nom hi]}})
          {:keys [start end worker]} (get record t)]
      (is (=? d start 0))
      (is (< (/ lo 2) end (* 2 hi)))))
  (testing "Record for a job with 2 tasks, 1 worker"
    (let [t1 "Task 1"
          t2 "Task 2"
          w "Megan"
          record (project-record {:backlog [{:id t1} {:id t2}]
                                  :estimates {t1 [:pert-3pt 1 2 5]
                                              t2 [:pert-3pt 2 3 8]}
                                  :workers #{w}})]
      (is (=? d 0 (get-in record [t1 :start])))
      (is (< 0 (get-in record [t2 :start])))
      (is (=? d
              (get-in record [t1 :end])
              (get-in record [t2 :start])))
      (is (= w
             (get-in record [t1 :worker])
             (get-in record [t2 :worker])))))
  (testing "Record for job with 2 tasks, 2 workers"
    (let [t1 "Task 1"
          t2 "Task 2"
          w1 "Megan"
          w2 "John"
          record (project-record {:backlog [{:id t1} {:id t2}]
                                  :estimates {t1 [:pert-3pt 1 2 5]
                                              t2 [:pert-3pt 2 3 8]}
                                  :workers #{w1 w2}})]
      (is (=? d 0 (get-in record [t1 :start]) (get-in record [t2 :start])))
      (is (not= (get-in record [t1 :worker])
                (get-in record [t2 :worker])))))
  (testing "Record for job with definite estimate"
    (let [t "Task 1"
          e 4
          w "John"]
      (is (= {t {:start 0, :end e, :worker w}}
             (project-record {:backlog [{:id t}]
                              :estimates {t [:definitely e]}
                              :workers #{w}})))))
  (testing "Record for job with 2 tasks with dependency, 1 worker"
    (let [t1 "Task 1", d1 2
          t2 "Task 2", d2 1
          w "Megan"
          record (project-record {:backlog [{:id t1, :deps #{t2}} {:id t2}]
                                  :estimates {t1 [:definitely d1]
                                              t2 [:definitely d2]}
                                  :workers #{w}})]
      (is (= record
             {t2 {:start 0
                  :end d2
                  :worker w}
              t1 {:start d2
                  :end (+ d1 d2)
                  :worker w}}))))
  (testing "Record for job with 2 tasks with dependency, 2 workers"
    (let [t1 "Task 1", d1 2
          t2 "Task 2", d2 1
          record (project-record {:backlog [{:id t1, :deps #{t2}} {:id t2}]
                                  :estimates {t1 [:definitely d1]
                                              t2 [:definitely d2]}
                                  :workers #{"Megan" "John"}})]
      (is (= (into {} (map (fn [[t r]] [t (select-keys r [:start :end])])) record)
             {t2 {:start 0
                  :end d2}
              t1 {:start d2
                  :end (+ d1 d2)}})))))

(deftest report-transformations
  (testing "Flip worker timelines"
    (is (= {0 [{:start 0, :end 2, :task "cut fur"}
               {:start 2, :end 8, :task "stuff fur"}
               {:start 8, :end 11, :task "dress bear"}
               {:start 11, :end 12, :task "package bear"}
               {:start 12, :end 13, :task "ship bear"}],
            1 [{:start 0, :end 1, :task "cut accessories"}
               {:start 1, :end 3, :task "sew accessories"}],
            7 [{:start 0, :end 2, :task "cut cloth"}
               {:start 2, :end 4, :task "sew clothes"}
               {:start 4, :end 6, :task "embroider"}]}
           (worker-timelines {"cut fur"
                              {:worker 0, :start 0, :end 2}
                              "stuff fur"
                              {:worker 0, :start 2, :end 8},
                              "cut cloth"
                              {:worker 7, :start 0, :end 2}
                              "sew clothes"
                              {:worker 7, :start 2, :end 4}
                              "embroider"
                              {:worker 7, :start 4, :end 6}
                              "cut accessories"
                              {:worker 1, :start 0, :end 1}
                              "sew accessories"
                              {:worker 1, :start 1, :end 3}
                              "dress bear"
                              {:worker 0, :start 8, :end 11}
                              "package bear"
                              {:worker 0, :start 11, :end 12},
                              "ship bear"
                              {:worker 0, :start 12, :end 13}})))))

(deftest full-examples
  (let [bear-record (project-record
                     (let [bear-process [["cut fur" #{} 2]
                                         ["stuff fur" #{"cut fur"} 6]
                                         ["cut cloth" #{} 2]
                                         ["sew clothes" #{"cut cloth"} 2]
                                         ["embroider" #{"sew clothes"} 2]
                                         ["cut accessories" #{} 1]
                                         ["sew accessories" #{"cut accessories"} 2]
                                         ["dress bear" #{"embroider" "stuff fur" "sew accessories"} 3]
                                         ["package bear" #{"dress bear"} 1]
                                         ["ship bear" #{"package bear"} 1]]]
                       {:backlog (map (fn [[id deps]] {:id id, :deps deps}) bear-process)
                        :estimates (into {}
                                         (map (fn [[id _ duration]]
                                                [id [:definitely duration]]))
                                         bear-process)
                        :workers (into #{} (range 10))}))
        timelines (worker-timelines bear-record)]
    (testing "Vermont teddy bear gantt chart from https://open.lib.umn.edu/exploringbusiness/chapter/11-4-graphical-tools-pert-and-gantt-charts"
      (testing "Completion times match"
        (is (= (into {}
                     (map (fn [[task completion]]
                            [task (select-keys completion [:start :end])]))
                     (seq bear-record))
               {"package bear" {:start 11, :end 12},
                "stuff fur" {:start 2, :end 8},
                "dress bear" {:start 8, :end 11},
                "sew accessories" {:start 1, :end 3},
                "embroider" {:start 4, :end 6},
                "cut accessories" {:start 0, :end 1},
                "cut cloth" {:start 0, :end 2},
                "ship bear" {:start 12, :end 13},
                "cut fur" {:start 0, :end 2},
                "sew clothes" {:start 2, :end 4}})))
      (testing "Worker timelines don't overlap"
        (is (not (some overlap? (vals timelines))))))))

(comment
  (with-open [writer (io/writer "test/bear.csv")]
    (csv/write-csv
     writer
     (cons ["ID" "Dependencies" "Estimate"]
           (map
            (fn [[id deps est]]
              [id (string/join ", " (seq deps)) est])
            [["cut fur" #{} 2]
             ["stuff fur" #{"cut fur"} 6]
             ["cut cloth" #{} 2]
             ["sew clothes" #{"cut cloth"} 2]
             ["embroider" #{"sew clothes"} 2]
             ["cut accessories" #{} 1]
             ["sew accessories" #{"cut accessories"} 2]
             ["dress bear" #{"embroider" "stuff fur" "sew accessories"} 3]
             ["package bear" #{"dress bear"} 1]
             ["ship bear" #{"package bear"} 1]]))))

  (let [ETE (csv->ETE "test/example.csv")]
    (random-variables/estimate 10000 ETE))

  ;; => {:mean 22.254977520216386, :std-dev 1.9733462549327425}
  ;; => {:mean 22.29659833024608, :std-dev 1.9838952653790716}
  ;; => {:mean 22.262001303616636, :std-dev 1.993646070808031}
  ;; => {:mean 22.285843249654775, :std-dev 1.983161076472538}

  (let [ETE (csv->ETE 2 "test/example.csv")]
    (random-variables/estimate 10000 ETE))
  ;; => {:mean 14.446350521352835, :std-dev 1.1685050075833878}
  ;; => {:mean 14.44700664775564, :std-dev 1.1546990523336935}
  ;; => {:mean 14.41511394253594, :std-dev 1.1545849358760427}
  ;; => {:mean 14.443845571824204, :std-dev 1.1686205560846399}

  (let [ETE (csv->ETE 20 "test/example.csv")]
    (random-variables/estimate 10000 ETE))
  ;; => {:mean 13.282937394321182, :std-dev 1.5063232138615634}
  ;; => {:mean 13.29439004821181, :std-dev 1.5016775269325153}
  ;; => {:mean 13.275560368899445, :std-dev 1.5052753901208547}
  ;; => {:mean 13.312250858647985, :std-dev 1.5181826174729272}

  {"d" {:worker "Megan", :start 1.323462834946807, :end 2.490815823384076},
   "f" {:worker "Megan", :start 4.490815823384076, :end 5.987796192462242},
   "e" {:worker "Megan", :start 2.490815823384076, :end 4.490815823384076},
   "j" {:worker "John", :start 12.508293580469951, :end 13.508293580469951},
   "a" {:worker "John", :start 0, :end 1.9648039531326515},
   "i" {:worker "John", :start 11.508293580469951, :end 12.508293580469951},
   "b" {:worker "John", :start 1.9648039531326515, :end 7.9421645467311635},
   "g" {:worker "Megan", :start 5.987796192462242, :end 8.508293580469951},
   "h" {:worker "John", :start 8.508293580469951, :end 11.508293580469951},
   "c" {:worker "Megan", :start 0, :end 1.323462834946807}})

(deftest refactoring
  (testing "examples for refactoring csv->simulator"
    (is (let [simulation ((csv->simulator #{1 2 3} "test/example.csv"))]
          (and (spec/valid? :pert.scheduling/projection simulation)
               (< 0 (count simulation))))))
  (testing "csv->ETE examples"
    (is (spec/valid? :pert.scheduling/duration
                     (random-variables/sample (csv->ETE "test/example.csv"))))
    ))

(deftest project-helpers
  (testing "duration sampling"
    (is (let [backlog (csv/backlog "test/example.csv")
              durations ((duration-sampler backlog))]
          (and
           (= (count backlog) (count durations))
           (spec/valid? (spec/every-kv :pert.task/id :pert.scheduling/duration)
                        ((duration-sampler (csv/backlog "test/example.csv"))))))))
  (testing "simulation stream is all projections"
    (is (spec/valid? (spec/coll-of :pert.scheduling/projection)
                     (take 5 (simulations (csv/backlog "test/example.csv") #{1 2})))))
  (testing "durations"
    (is (spec/valid? (spec/coll-of (spec/every-kv :pert.task/id :pert.scheduling/duration))
                     (take 5 (durations (csv/backlog "test/example.csv")))))))

