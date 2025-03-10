(ns pert.excel
  (:require
   [clojure.spec.alpha :as spec]
   [dk.ative.docjure.spreadsheet :as docjure]
   [pert.spreadsheet :as spreadsheet]
   ))


(defn sheet-names [workbook]
  (map docjure/sheet-name (docjure/sheet-seq workbook)))


(defn raw-rows [filepath]
  (->> (docjure/load-workbook filepath)
       (docjure/select-sheet (fn [_] true))
       docjure/row-seq
       (remove nil?)
       (map docjure/cell-seq)
       (map #(map docjure/read-cell %))))


(defn rows
  [filepath]
  (spreadsheet/rows (raw-rows filepath)))


(defn backlog
  "Task backlog from the provided Excel file."
  ([excel-file] (backlog {} excel-file))
  ([column-overrides excel-file]
   (spreadsheet/backlog column-overrides (rows excel-file))))


(spec/fdef backlog
  :ret :pert.task/backlog)
