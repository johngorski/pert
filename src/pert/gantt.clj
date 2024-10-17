(ns pert.gantt
  (:require
   [clojure.math :as math]
   [clojure.string :as string]
   [hiccup2.core :as hiccup]
   [pert.csv :as csv]
   [pert.random-variables :as random-variables]
   [pert.scheduling :as scheduling]
   ))

(defn status->rgb
  "Get RGB color values from the chance of the task starting and the chance of it finishing.
  Not started: Red (0xFF0000). In-progress: Yellow (0xFFFF00). Finished: Green (0x00FF00)."
  [chance-started chance-finished]
  [(int (math/ceil (* 255 (- 1 chance-finished))))
   (int (math/ceil (* 255 chance-started)))
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
        [r-h y-h g-h] (map (fn [x]
                             (int (* s x)))
                           (status-heights chance-started chance-finished))]
    [:svg {:width s :height s :xmlns "http://www.w3.org/2000/svg"}
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

(defn box
  "Hiccup data for a box of color given as RGB vector on [0, 255]."
  [[r g b]]
  (let [s 18]
    [:svg {:width s :height s}
     [:rect {:x 0 :y 0 :width s :height s :style (str "fill: rgb(" r "," g "," b ")")}]]))

(defn project-duration
  "The final end time of all tasks in the provided project simulation."
  [sim]
  (reduce max (map :end (vals sim))))

(defn day-frequencies
  [samples]
  (let [ids (keys (first samples))]
    (into {}
          (map
           (fn [id]
             (let [day-frequencies
                   (fn [endpoint]
                     (frequencies
                      (map
                       (fn [sample]
                         (int (math/ceil (get-in sample [id endpoint]))))
                       samples)))]
               [id {:start (day-frequencies :start)
                    :end (day-frequencies :end)}])))
          ids)))

(defn last-day
  [day-frequencies]
  (reduce max
          (mapcat
           (fn [[_ {:keys [end]}]]
             (keys end))
           day-frequencies)))

(defn cdf
  "Discrete cumulative distribution function at the bucket level."
  [histogram buckets]
  (reductions + (map #(get histogram % 0) buckets)))

(defn gantt
  "Get Gantt chart statistic summaries based on the provided project simulations.
  You know what would be great for that? A map from task ID to parallel sequences
  of start and end times. Or the histogram data right there.
  "
  [samples]
  (let [task-day-histogram (day-frequencies samples)
        sample-count (reduce + (-> task-day-histogram first second :start vals))
        days (range (inc (last-day task-day-histogram)))
        ]
    (into {}
          (map (fn [[id {:keys [start end]}]]
                 [id (let [started (cdf start days)
                           finished (cdf end days)]
                       (map (fn [started-count finished-count]
                              {:queued (- sample-count started-count)
                               :in-progress (- started-count finished-count)
                               :finished finished-count})
                            started finished))]))
          (seq task-day-histogram))
    ))

(def cell-visuals
  {:gradient (comp box status->rgb)
   :bar status->svg})

(defn hiccup
  ([backlog data] (hiccup {:cell-visual :gradient} backlog data))
  ([props backlog data]
   (let [row (fn [cells] [:tr cells])
         header (row
                 (cons [:th "Day"]
                       (map-indexed (fn [idx _]
                                      [:th
                                       ;; TODO: calculate in-progress tasks here
                                       ;; (let [in-progress
                                       ;; all ]
                                       ;; {:title (format "%.1f in-progress tasks"
                                       ;; (* 1.0 (/ in-progress all)))})
                                       idx])
                                    (first (vals data)))))
         task-row (fn [{:keys [id title description]}]
                    (row
                     (cons [:th {:title description} title]
                           (map-indexed
                            (fn [idx {:keys [queued in-progress finished]}]
                              (let [sim-count (+ queued in-progress finished)
                                    percent (fn [n] (format "%.1f%%" (* 100.0 (/ n sim-count))))]
                                [:td
                                 {:title (string/join "\n" [title
                                                            (str "Day " idx)
                                                            (str (percent queued) " queued")
                                                            (str (percent in-progress) " in-progress")
                                                            (str (percent finished) " finished")])}
                                 ((get cell-visuals (:cell-visual props))
                                  (/ (+ in-progress finished) sim-count)
                                  (/ finished sim-count))
                                 ]))
                            (get data id))
                           )))]
     [:table {:cellpadding 1 :cellspacing 0}
      header
      (map (comp task-row) backlog)]
     )))

(defn gantt-hiccup
  ([backlog duration-samples team]
   (gantt-hiccup {:cell-visual :gradient} backlog duration-samples team))

  ([props backlog duration-samples team]
   (hiccup props
           backlog
           (gantt
            (map (fn [durations]
                   (scheduling/project backlog durations team))
                 duration-samples)))))

(defn gantt-html
  ([backlog duration-samples team]
   (gantt-html {:cell-visual :gradient} backlog duration-samples team))
  ([props backlog duration-samples team]
   (str (hiccup/html (gantt-hiccup props backlog duration-samples team)))))
