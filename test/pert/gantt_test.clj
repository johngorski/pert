(ns pert.gantt-test
  (:require
   [clojure.test :refer :all]
   [pert.csv :as csv]
   [pert.gantt :refer :all]
   [pert.scheduling :as scheduling]
   ))

;; TODO: Move durations to the right namespace, maybe scheduling?
;; gantt is the wrong namespace
(deftest durations
  (testing "project duration is maximum of all task end times"
    (is (= 7 (project-duration {"a" {:end 1}
                                "b" {:end 7}
                                "c" {:end 4}})))))

(def backlog (csv/backlog "test/example.csv"))

(def test-samples
  ;; (take 3 (scheduling/simulations backlog #{1 2}))
  '({"d" {:worker 2, :start 1.6942905772305425, :end 3.0798003550878157},
     "f" {:worker 2, :start 5.079800355087816, :end 5.60458112977136},
     "e" {:worker 2, :start 3.0798003550878157, :end 5.079800355087816},
     "j" {:worker 1, :start 15.994847574304341, :end 16.99484757430434},
     "a" {:worker 1, :start 0, :end 1.6141238680054748},
     "i" {:worker 1, :start 14.994847574304341, :end 15.994847574304341},
     "b" {:worker 1, :start 1.6141238680054748, :end 11.994847574304341},
     "g" {:worker 2, :start 5.60458112977136, :end 7.484233219600716},
     "h" {:worker 1, :start 11.994847574304341, :end 14.994847574304341},
     "c" {:worker 2, :start 0, :end 1.6942905772305425}}
    {"d" {:worker 2, :start 1.4715943258734325, :end 3.5530108019313342},
     "f" {:worker 2, :start 5.553010801931334, :end 6.264607743472113},
     "e" {:worker 2, :start 3.5530108019313342, :end 5.553010801931334},
     "j" {:worker 1, :start 11.520886549449209, :end 12.520886549449209},
     "a" {:worker 1, :start 0, :end 1.6937883126842388},
     "i" {:worker 1, :start 10.520886549449209, :end 11.520886549449209},
     "b" {:worker 1, :start 1.6937883126842388, :end 7.033559283729118},
     "g" {:worker 2, :start 6.264607743472113, :end 7.520886549449209},
     "h" {:worker 1, :start 7.520886549449209, :end 10.520886549449209},
     "c" {:worker 2, :start 0, :end 1.4715943258734325}}
    {"d" {:worker 2, :start 2.688262083056106, :end 7.136066883600771},
     "f" {:worker 2, :start 9.13606688360077, :end 9.724910722070884},
     "e" {:worker 2, :start 7.136066883600771, :end 9.13606688360077},
     "j" {:worker 1, :start 15.92250078198598, :end 16.92250078198598},
     "a" {:worker 1, :start 0, :end 1.5630434335101195},
     "i" {:worker 1, :start 14.92250078198598, :end 15.92250078198598},
     "b" {:worker 1, :start 1.5630434335101195, :end 10.325729264270246},
     "g" {:worker 2, :start 9.724910722070884, :end 11.92250078198598},
     "h" {:worker 1, :start 11.92250078198598, :end 14.92250078198598},
     "c" {:worker 2, :start 0, :end 2.688262083056106}}))

(def example-day-frequencies
  '{"d" {:start {2 2, 3 1},
         :end {4 2, 8 1}},
    "f" {:start {6 2, 10 1},
         :end {6 1, 7 1, 10 1}},
    "e" {:start {4 2, 8 1},
         :end {6 2, 10 1}},
    "j" {:start {16 2, 12 1},
         :end {17 2, 13 1}},
    "a" {:start {0 3},
         :end {2 3}},
    "i" {:start {15 2, 11 1},
         :end {16 2, 12 1}},
    "b" {:start {2 3},
         :end {12 1, 8 1, 11 1}},
    "g" {:start {6 1, 7 1, 10 1},
         :end {8 2, 12 1}},
    "h" {:start {12 2, 8 1},
         :end {15 2, 11 1}},
    "c" {:start {0 3},
         :end {2 2, 3 1}}})

(deftest examples
  (testing "example project gantt data match static examples"
    ;; TODO: This output format isn't so great, what about some histograms here?
    ;; Parallel stacked histograms, maybe.
    #_({id (spec/seq-of {:not-started ::number, :started ::number, :finished ::number})})
    ;; That'd be kind of neat. That backs any day-based gantt chart we want.
    ;; TODO: Yeah, do that!
    ;;       We're almost there with the start/end day frequency counts.
    ;;       Last shot is accumulating them to day-grained CDFs.
    (testing "day frequencies"
      (is (= (day-frequencies test-samples)
             example-day-frequencies)))))

;; Cool, histogram data for start/end.
;; Range of days is the max of the end keys
;; We can scan through each day of the project calculating how many simulations
;; being in which state on that day.
;; Since we're checking each day we don't need to sort up-front.

(last-day example-day-frequencies)
;; => 17

(gantt test-samples)
'{"d" ({:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 1, :in-progress 2, :finished 0} {:queued 0, :in-progress 3, :finished 0} {:queued 0, :in-progress 1, :finished 2} {:queued 0, :in-progress 1, :finished 2} {:queued 0, :in-progress 1, :finished 2} {:queued 0, :in-progress 1, :finished 2} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3}), "f" ({:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 1, :in-progress 1, :finished 1} {:queued 1, :in-progress 0, :finished 2} {:queued 1, :in-progress 0, :finished 2} {:queued 1, :in-progress 0, :finished 2} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3}), "e" ({:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 1, :in-progress 2, :finished 0} {:queued 1, :in-progress 2, :finished 0} {:queued 1, :in-progress 0, :finished 2} {:queued 1, :in-progress 0, :finished 2} {:queued 0, :in-progress 1, :finished 2} {:queued 0, :in-progress 1, :finished 2} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3}), "j" ({:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 2, :in-progress 1, :finished 0} {:queued 2, :in-progress 0, :finished 1} {:queued 2, :in-progress 0, :finished 1} {:queued 2, :in-progress 0, :finished 1} {:queued 0, :in-progress 2, :finished 1} {:queued 0, :in-progress 0, :finished 3}), "a" ({:queued 0, :in-progress 3, :finished 0} {:queued 0, :in-progress 3, :finished 0} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3}), "i" ({:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 2, :in-progress 1, :finished 0} {:queued 2, :in-progress 0, :finished 1} {:queued 2, :in-progress 0, :finished 1} {:queued 2, :in-progress 0, :finished 1} {:queued 0, :in-progress 2, :finished 1} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3}), "b" ({:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 0, :in-progress 3, :finished 0} {:queued 0, :in-progress 3, :finished 0} {:queued 0, :in-progress 3, :finished 0} {:queued 0, :in-progress 3, :finished 0} {:queued 0, :in-progress 3, :finished 0} {:queued 0, :in-progress 3, :finished 0} {:queued 0, :in-progress 2, :finished 1} {:queued 0, :in-progress 2, :finished 1} {:queued 0, :in-progress 2, :finished 1} {:queued 0, :in-progress 1, :finished 2} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3}), "g" ({:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 2, :in-progress 1, :finished 0} {:queued 1, :in-progress 2, :finished 0} {:queued 1, :in-progress 0, :finished 2} {:queued 1, :in-progress 0, :finished 2} {:queued 0, :in-progress 1, :finished 2} {:queued 0, :in-progress 1, :finished 2} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3}), "h" ({:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 3, :in-progress 0, :finished 0} {:queued 2, :in-progress 1, :finished 0} {:queued 2, :in-progress 1, :finished 0} {:queued 2, :in-progress 1, :finished 0} {:queued 2, :in-progress 0, :finished 1} {:queued 0, :in-progress 2, :finished 1} {:queued 0, :in-progress 2, :finished 1} {:queued 0, :in-progress 2, :finished 1} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3}), "c" ({:queued 0, :in-progress 3, :finished 0} {:queued 0, :in-progress 3, :finished 0} {:queued 0, :in-progress 1, :finished 2} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3} {:queued 0, :in-progress 0, :finished 3})}

(map (fn [task-day] (map (fn [day] (reduce + (vals day))) task-day)) (vals (gantt test-samples)))
'((3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3)
  (3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3)
  (3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3)
  (3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3)
  (3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3)
  (3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3)
  (3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3)
  (3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3)
  (3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3)
  (3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3))


(comment
  '{"d" {:started (0 0 2 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3),
         :finished (0 0 0 0 2 2 2 2 3 3 3 3 3 3 3 3 3 3)},
    "f" {:started (0 0 0 0 0 0 2 2 2 2 3 3 3 3 3 3 3 3),
         :finished (0 0 0 0 0 0 1 2 2 2 3 3 3 3 3 3 3 3)},
    "e" {:started (0 0 0 0 2 2 2 2 3 3 3 3 3 3 3 3 3 3),
         :finished (0 0 0 0 0 0 2 2 2 2 3 3 3 3 3 3 3 3)},
    "j" {:started (0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 1 3 3),
         :finished (0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 1 3)}
    "a" {:started (3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3)
         :finished (0 0 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3)}
    "i" {:started (0 0 0 0 0 0 0 0 0 0 0 1 1 1 1 3 3 3)
         :finished (0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 1 3 3)}
    "b" {:started (0 0 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3)
         :finished (0 0 0 0 0 0 0 0 1 1 1 2 3 3 3 3 3 3)},
    "g" {:started (0 0 0 0 0 0 1 2 2 2 3 3 3 3 3 3 3 3)
         :finished (0 0 0 0 0 0 0 0 2 2 2 2 3 3 3 3 3 3)}
    "h" {:started (0 0 0 0 0 0 0 0 1 1 1 1 3 3 3 3 3 3),
         :finished (0 0 0 0 0 0 0 0 0 0 0 1 1 1 1 3 3 3)},
    "c" {:started (3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3),
         :finished (0 0 2 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3)}})

(hiccup backlog (gantt test-samples))
'(:table {:cellpadding 1, :cellspacing 0}
         (:tr [:th "Day"])
         (:tr [:th {:title "Cut fur to the shape of the needed bear."} "Cut fur"])
         (:tr [:th {:title "Stuff the cut fur to the right density for the quality of the product."} "Stuff fur"])
         (:tr [:th {:title "Cut cloth for the bear’s size"} "Cut cloth"])
         (:tr [:th {:title "Sew cloth into bear clothing"} "Sew clothes"])
         (:tr [:th {:title "Embroider custom name and message on the bear"} "Embroider"])
         (:tr [:th {:title "Cut ordered bear accessories"} "Cut accessories"])
         (:tr [:th {:title "Attach accessories to the bear"} "Sew accessories"])
         (:tr [:th {:title "Dress bear in custom clothing"} "Dress bear"])
         (:tr [:th {:title "Package bear for stackable shipping"} "Package bear"])
         (:tr [:th {:title "Transport bear to shipping"} "Ship bear"]))



