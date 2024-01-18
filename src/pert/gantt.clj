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
  (let [s 18
        [r-h y-h g-h] (map (fn [x] (int (* s x))) (status-heights chance-started chance-finished))]
    [:svg {:width s :height s}
     [:rect {:x 0 :y 0 :width s :height g-h
             ;; :style "fill:#ffffff"}] ;; done
             :style "fill:#00ff00"}] ;; done
     [:rect {:x 0 :y g-h :width s :height y-h
             ;; :style "fill:#000000"}] ;; in-progress
             :style "fill:#ffff00"}] ;; in-progress
     [:rect {:x 0 :y (+ g-h y-h) :width s :height r-h
             ;; :style "fill:#ffffff"}] ;; not started
             :style "fill:#ff0000"}] ;; not started
     ]
    ))

(defn task-svg
  ""
  [start-cdf finish-cdf]
  (fn [t]
    (status->svg (start-cdf t) (finish-cdf t))))

(defn task-gradient
  "Gradient based on cdfs for a task starting and task finishing."
  [start-cdf finish-cdf]
  (fn [t]
    (status->rgb (start-cdf t) (finish-cdf t))))

(defn box
  "Hiccup data for a box of color given as RGB vector on [0, 255]."
  [[r g b]]
  (let [s 18]
    [:td [:svg {:width s :height s}
          [:rect {:x 0 :y 0 :width s :height s :style (str "fill: rgb(" r "," g "," b ")")}]]]))
;;  [:td {:style (str "background-color: rgb(" r "," g "," b ")")}])

(comment
  (str (hiccup/html
        (box [255 255 0]))))
;; TODO: replace references in scheduling_test.clj with ones from above

(defn csv->simulator [workers in-csv]
  (let [rows (scheduling/csv->rows in-csv)
        backlog (scheduling/rows->backlog rows)
        estimates (scheduling/rows->3pt-estimates rows)]
    #(scheduling/project-record {:backlog backlog
                                 :estimates estimates
                                 :workers workers})))

(defn project-duration
  "The final end time of all tasks in the provided project simulation."
  [sim]
  (reduce max (map :end (vals sim))))

(comment
  (reduce max (map :end (vals
                         ((csv->simulator #{1 2} "test/example.csv")))))

  (project-duration ((csv->simulator #{1 2} "test/example.csv"))))

(defn csv->gantt-html
  ([in-csv] (csv->gantt-html 1 in-csv))
  ([worker-count in-csv]
   (let [rows (scheduling/csv->rows in-csv)
         backlog (scheduling/rows->backlog rows)
         simulate (csv->simulator (into #{} (range worker-count)) in-csv)

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

         days (range (Math/ceil (reduce max (map project-duration samples))))

         header [:tr [:th "Day"] (sequence (map (fn [day] [:th (str day)])) days)]

         task-row (fn [task]
                    [:tr
                     [:th task]
                     (sequence (map (fn [day] (box ((gradients task) day)))) days)])
         ]
     (str (hiccup/html {:mode :html}
                       [:html
                        [:body
                         [:table {:cellpadding 1 :cellspacing 0}
                          header
                          (map (comp task-row :id) backlog)
                          ]]])))))

(defn csv->gantt-bar-html
  [in-csv]
  (let [rows (scheduling/csv->rows in-csv)
        backlog (scheduling/rows->backlog rows)
        estimates (scheduling/rows->3pt-estimates rows)
        simulate #(scheduling/project-record {:backlog backlog
                                              :estimates estimates
                                              :workers (into #{} (range 3))})

        samples (repeatedly 10000 simulate)

        start-cdf-for (fn [task]
                        (random-variables/interpolate-cdf
                         (map (fn [sim] (get-in sim [task :start]))
                              samples)))

        end-cdf-for (fn [task]
                      (random-variables/interpolate-cdf
                       (map (fn [sim] (get-in sim [task :end]))
                            samples)))

        svgs-for (fn [task]
                   (task-svg (start-cdf-for task) (end-cdf-for task)))

        svgs (into {} (map (fn [{:keys [id]}] [id (svgs-for id)])) backlog)

        days (range 30) ;; TODO: Get from max of samples. Days may only make sense so far.

        header [:tr [:th "Day"] (sequence (map (fn [day] [:th (str day)])) days)]

        task-row (fn [task]
                   [:tr
                    [:th task]
                    (sequence (map (fn [day] [:td ((svgs task) day)])) days)])
        ]
    (str (hiccup/html {:mode :html}
                      [:html
                       [:body
                        [:table {:cellpadding 1 :cellspacing 0}
                         header
                         (map (comp task-row :id) backlog)
                         ]]]))))

(comment
  (spit "/Users/jgorski/Desktop/gantt.html"
        (csv->gantt-html "/Users/jgorski/Downloads/estimates.csv"))

  (spit "/Users/jgorski/Desktop/gantt.html"
        (csv->gantt-html "test/example.csv"))

  (spit "/Users/jgorski/Desktop/gantt-bar.html"
        (csv->gantt-bar-html "test/example.csv"))

  )

