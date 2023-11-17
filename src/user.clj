(ns user
  (:require
   [nextjournal.clerk :as clerk]
   ))

(defn start-clerk! []
  (clerk/serve!
   {:browse? true
    :watch-paths ["notebooks"]}))

(println)
(println)
(println ">>> Run (start-clerk!) to browse notebooks.")
(println)
(println)


