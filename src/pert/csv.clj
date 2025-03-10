(ns pert.csv
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.spec.alpha :as spec]
   [pert.spreadsheet :as spreadsheet]
   [pert.task])
  (:import (org.apache.commons.io.input BOMInputStream)))

(defn rows
  "Right so maybe not \"rows,\" but maybe task data? To Tasks (since everything is data)?
  That's kind of what a row is anyway, we just don't have any definition around it.
  That's what specs are for, right?"
  [filepath]
  (with-open [reader (io/reader (-> filepath io/input-stream BOMInputStream. io/reader))]
    (spreadsheet/rows (csv/read-csv reader))))


;; TODO: Migrate this to spreadsheet namespace, since this is valid for anything that starts with
;; rows, e.g. Excel or TSV files.


(defn parse-dependencies
  "Parse dependency cell into set of dependency IDs."
  ([s] (spreadsheet/parse-dependencies #"\s*,\s*" s))
  ([separator s]
   (spreadsheet/parse-dependencies separator s)))


(defn task
  "Project task defined by the given row.
  id, title, description, deps, estimate"
  ([row] (spreadsheet/task {} row))
  ([column-mapper-overrides row]
   (spreadsheet/task column-mapper-overrides row)))

(spec/fdef task
  :ret :pert.task/task)

(defn backlog
  "Task backlog from the provided CSV file."
  ([csv-file] (backlog {} csv-file))
  ([column-overrides csv-file]
   (spreadsheet/backlog column-overrides (rows csv-file))))


(spec/fdef backlog
  :ret :pert.task/backlog)
