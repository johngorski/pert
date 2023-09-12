(ns pert.gantt
  (:require
   [hiccup2.core :as hiccup]
   [pert.random-variables :as random-variables]
   [pert.scheduling :as scheduling]
   ))

;; TODO: replace references in scheduling_test.clj with ones from above
(defn status->rgb
  "Get RGB color values from the chance of the task starting and the chance of it finishing.
  Not started: Red (0xFF0000). In-progress: Yellow (0xFFFF00). Finished: Green (0x00FF00)."
  [chance-started chance-finished]
  [(int (Math/ceil (* 255 (- 1 chance-finished))))
   (int (Math/ceil (* 255 chance-started)))
   0])

(defn status-heights
  "Divides 1.0 based on chance started and finished. Becomes relative heights of not started/in-progress/done."
  [chance-started chance-finished]
  (let [chance-not-started (- 1.0 chance-started chance-finished)]
    [chance-not-started chance-started chance-finished]))

(defn status->svg
  "hiccup data for svg box with heights based on task status.
  Not started = red on top, in-progress = yellow in middle, done = green in remainder."
  [chance-started chance-finished]
  (let [s 20
        [r-h y-h g-h] (map (fn [x] (int (* s x))) (status-heights chance-started chance-finished))]
    [:svg
     [:rect {:x 0 :y 0 :width s :height r-h
             :style "fill:red"}] ;; not started
     [:rect {:x 0 :y r-h :width s :height y-h
             :style "fill:yellow"}] ;; in-progress
     [:rect {:x 0 :y (+ r-h y-h) :width s :height g-h
             :style "fill:green"}] ;; done
     ]
    ))

(defn task-gradient
  "Gradient based on cdfs for a task starting and task finishing."
  [start-cdf finish-cdf]
  (fn [t]
    (status->rgb (start-cdf t) (finish-cdf t))))

(defn box
  "Hiccup data for a box of color given as RGB vector on [0, 255]."
  [[r g b]]
  [:td {:style (str "background-color: rgb(" r "," g "," b ")")}])

(comment
  (str (hiccup/html
        (box [255 255 0]))))
;; TODO: replace references in scheduling_test.clj with ones from above


(defn csv->gantt-html
  [in-csv]
  (let [rows (scheduling/csv->rows in-csv)
        backlog (scheduling/rows->backlog rows)
        estimates (scheduling/rows->3pt-estimates rows)
        simulate #(scheduling/project-record {:backlog backlog
                                              :estimates estimates
                                              :workers (into #{} (range 2))})

        samples (repeatedly 10000 simulate)

        start-cdf-for (fn [task]
                        (random-variables/interpolate-cdf
                         (map (fn [sim] (get-in sim [task :start]))
                              samples)))

        end-cdf-for (fn [task]
                      (random-variables/interpolate-cdf
                       (map (fn [sim] (get-in sim [task :end]))
                            samples)))

        gradient-for  (fn [task]
                        (task-gradient (start-cdf-for task) (end-cdf-for task)))

        gradients (into {} (map (fn [{:keys [id]}] [id (gradient-for id)])) backlog)

        days (range 20) ;; TODO: Get from max of samples. Days may only make sense so far.

        header [:tr [:th "Day"] (sequence (map (fn [day] [:th (str day)])) days)]

        task-row (fn [task]
                   [:tr
                    [:th task]
                    (sequence (map (fn [day] (box ((gradients task) day)))) days)])
        ]
    (str (hiccup/html {:mode :html}
                      [:html
                       [:body
                        [:table
                         header
                         (map (comp task-row :id) backlog)
                         ]]]))))

(comment
  (spit "/Users/jgorski/Desktop/gantt.html"
        (csv->gantt-html "/Users/jgorski/Downloads/estimates.csv"))

  (spit "/Users/jgorski/Desktop/gantt.html"
        (csv->gantt-html "test/example.csv")))

