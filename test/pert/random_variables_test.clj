(ns pert.random-variables-test
  (:require
   [clojure.test :refer :all]
   [pert.random-variables :refer :all]))

(comment
  (sample (construct [:definitely 4]))
  ;; => 4
  ;; => 4
  ;; => 4

  (sample (construct [:pert-3pt 1 2 5]))
  ;; => 1.7550671864129304
  ;; => 1.0106718955178169
  ;; => 1.533958861532001

  (sample (construct [:pert-3pt 1 4 5])))
;; => 4.4282161430422065
;; => 3.8439939699458647
;; => 3.9472082840821847

(deftest combinations
  (testing "can use :fn tag to add"
    (is (= 5
           (sample (construct [:fn + [:definitely 2] [:definitely 3]]))))
    (testing "can find max with :fn"
      (is (= 9
             (sample (construct [:fn max [:definitely 8] [:definitely 9]])))))))

(comment
  (sample (construct [:D 6]))

  (repeat 2 (construct [:D 6]))

  (concat [:fn +] (repeat 2 (construct [:D 6])))

  (sample (construct [:x 2 [:D 6]]))

  (let [x2d6 (construct [:x 2 [:D 6]])]
    (sort-by first (frequencies (repeatedly (* 11 11 11) #(sample x2d6))))))
;; => ([2 29] [3 63] [4 96] [5 165] [6 201] [7 226] [8 193] [9 165] [10 86] [11 71] [12 36])
;; => ([2 33] [3 58] [4 120] [5 141] [6 203] [7 232] [8 184] [9 150] [10 112] [11 55] [12 43])
;; => ([2 39] [3 87] [4 102] [5 133] [6 188] [7 225] [8 185] [9 150] [10 112] [11 67] [12 43])
;; => ([2 45] [3 81] [4 120] [5 153] [6 179] [7 218] [8 190] [9 125] [10 116] [11 65] [12 39])
;; => ([2 44] [3 64] [4 128] [5 153] [6 206] [7 219] [8 178] [9 143] [10 112] [11 50] [12 34])
;; => ([2 47] [3 76] [4 125] [5 147] [6 191] [7 195] [8 185] [9 153] [10 103] [11 75] [12 34])
;; => ([2 43] [3 65] [4 102] [5 160] [6 203] [7 213] [8 188] [9 149] [10 99] [11 58] [12 51])

(defn volley-outcome [attacker-dice defender-dice]
  (frequencies (map
                (fn [a d] (if (< d a) :attacker :defender))
                (sort-by - attacker-dice)
                (sort-by - defender-dice))))

(comment
  (volley-outcome [3 6 3] [4 5])
  ;; => {:attacker 1, :defender 1}
  (volley-outcome [1 1 1] [4 5])
  ;; => {:defender 2}
  (volley-outcome [6 6 6] [6 6])
  ;; => {:defender 2}
  (volley-outcome [6 6 6] [6])
  ;; => {:defender 1}
  (volley-outcome [3] [2 3])
  ;; => {:defender 1}

  (vector 1 2)

  (let [risk-volley (construct [:fn volley-outcome
                                [:fn vector [:D 6] [:D 6] [:D 6]]
                                [:fn vector [:D 6] [:D 6]]])]
    (frequencies (repeatedly 10000 #(sample risk-volley)))))
;; => {{:attacker 1, :defender 1} 3387, {:attacker 2} 3690, {:defender 2} 2923}
;; => {{:attacker 2} 3787, {:defender 1, :attacker 1} 3325, {:defender 2} 2888}

(defn dice [n] (concat [:fn vector] (repeat n [:D 6])))

(defn risk-volley [attacking defending]
  (construct [:fn volley-outcome (dice attacking) (dice defending)]))

(defn risk-battle [attackers defenders]
  (if (or (< attackers 2)
          (< defenders 1))
    {:attackers attackers
     :defenders defenders}
    (let [attacking (min 3 (dec attackers))
          defending (min 2 defenders)
          {:keys [attacker defender]
           :or {attacker 0 defender 0}}
          (sample (risk-volley attacking defending))]
      (recur (- attackers defender) (- defenders attacker)))))

(comment
  (risk-battle 3 2)
  ;; => {:attackers 3, :defenders 0}
  ;; => {:attackers 1, :defenders 2}
  ;; => {:attackers 3, :defenders 0}
  ;; => {:attackers 3, :defenders 0}
  ;; => {:attackers 1, :defenders 2}
  ;; => {:attackers 3, :defenders 0}
  ;; => {:attackers 1, :defenders 2}

  (sort-by (comp - second) (frequencies (repeatedly 10000 #(risk-battle 10 2))))
  ([{:attackers 10, :defenders 0} 3697]
   [{:attackers 9, :defenders 0} 2139]
   [{:attackers 8, :defenders 0} 1928]
   [{:attackers 7, :defenders 0} 873]
   [{:attackers 6, :defenders 0} 641]
   [{:attackers 5, :defenders 0} 299]
   [{:attackers 4, :defenders 0} 204]
   [{:attackers 3, :defenders 0} 82]
   [{:attackers 1, :defenders 2} 64]
   [{:attackers 1, :defenders 1} 48]
   [{:attackers 2, :defenders 0} 25])

  (sort-by (comp - second) (frequencies (repeatedly 10000 #(risk-battle 10 10))))
  ([{:attackers 4, :defenders 0} 934]
   [{:attackers 5, :defenders 0} 893]
   [{:attackers 1, :defenders 3} 873]
   [{:attackers 1, :defenders 2} 818]
   [{:attackers 1, :defenders 4} 777]
   [{:attackers 6, :defenders 0} 756]
   [{:attackers 1, :defenders 5} 734]
   [{:attackers 3, :defenders 0} 640]
   [{:attackers 1, :defenders 6} 614]
   [{:attackers 7, :defenders 0} 605]
   [{:attackers 1, :defenders 7} 441]
   [{:attackers 1, :defenders 1} 421]
   [{:attackers 8, :defenders 0} 374]
   [{:attackers 2, :defenders 0} 312]
   [{:attackers 1, :defenders 8} 289]
   [{:attackers 9, :defenders 0} 209]
   [{:attackers 1, :defenders 9} 182]
   [{:attackers 10, :defenders 0} 75]
   [{:attackers 1, :defenders 10} 53])

  (sort-by (comp - second) (frequencies (repeatedly 10000 #(risk-battle 10 5))))
  ([{:attackers 8, :defenders 0} 1453]
   [{:attackers 7, :defenders 0} 1444]
   [{:attackers 6, :defenders 0} 1234]
   [{:attackers 9, :defenders 0} 1186]
   [{:attackers 5, :defenders 0} 1040]
   [{:attackers 10, :defenders 0} 954]
   [{:attackers 4, :defenders 0} 780]
   [{:attackers 3, :defenders 0} 480]
   [{:attackers 1, :defenders 2} 476]
   [{:attackers 1, :defenders 3} 272]
   [{:attackers 1, :defenders 1} 243]
   [{:attackers 2, :defenders 0} 219]
   [{:attackers 1, :defenders 4} 156]
   [{:attackers 1, :defenders 5} 63]))
