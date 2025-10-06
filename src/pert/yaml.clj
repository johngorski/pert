(ns pert.yaml
  (:require
   [clj-yaml.core :as yaml]
   [clojure.set :as set]
   ))


(yaml/parse-string "
- {name: John Smith, age: 33}
- name: Mary Smith
  age: 27
")


(def breakdown
  (yaml/parse-string "
Tasks:
- Dress bear:
  - Clothes:
    - Cut cloth
    - Sew clothes
    - Embroider
  - Accessories:
    - Cut accessories
    - Sew accessories
  - Fur:
    - Cut fur
    - Stuff fur
- Package bear
- Ship bear

" :keywords false))


(get breakdown "Tasks")
(comment
  (#ordered/map (["Dress bear"
                  (#ordered/map (["Clothes"
                                  ("Cut cloth" "Sew clothes" "Embroider")])
                   #ordered/map (["Accessories"
                                  ("Cut accessories" "Sew accessories")])
                   #ordered/map (["Fur"
                                  ("Cut fur" "Stuff fur")]))])
   "Package bear"
   "Ship bear"))



(seq (get breakdown "Tasks"))
(keys {:a 1 :b 2})
(keys (first (get breakdown "Tasks")))
(map? (first (get breakdown "Tasks")))

(map first
 (:Tasks breakdown))

;; Let's set up the dependencies here. Let's say the tree is

(def dep-yaml
  "
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
")

(def parsed-yaml
  (get (yaml/parse-string dep-yaml :keywords false) "Tasks"))

;; The default is that, at the same depth, we want later tasks to depend on previous tasks.

(def yaml-prev-deps
  {
   "1" #{}
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
   "4.3" #{"4.1" "4.2"}
   })

;; You'd also want the subtasks to depend on the prerequisites for their parent, other than
;; the subtasks themselves:

(def yaml-parent-deps
  {
   "1" #{}
   "1.1" #{}
   "1.2" #{}
   "2" #{}
   "2.1" #{"1"}
   "2.2" #{"1"}
   "2.2.1" #{"2.1"}
   "2.2.2" #{"2.1"}
   "2.3" #{"1"}
   "3" #{}
   "3.1" #{"1" "2"}
   "4" #{}
   "4.1" #{"1" "2" "3"}
   "4.2" #{"1" "2" "3"}
   "4.3" #{"1" "2" "3"}
   })

;; The deeper depth are sub-tasks, and the outer task should depend on all subtasks.

(def yaml-child-deps
  {
   "1" #{"1.1" "1.2"}
   "1.1" #{}
   "1.2" #{}
   "2" #{"2.1" "2.2" "2.3"}
   "2.1" #{}
   "2.2" #{"2.2.1" "2.2.2"}
   "2.2.1" #{}
   "2.2.2" #{}
   "2.3" #{}
   "3" #{"3.1"}
   "3.1" #{}
   "4" #{"4.1" "4.2" "4.3"}
   "4.1" #{}
   "4.2" #{}
   "4.3" #{}
   })

;; That would look like the following deps adjacency list:
(def yaml-deps
  (merge-with set/union
              yaml-prev-deps
              yaml-parent-deps
              yaml-child-deps
              ))

yaml-deps
{"3" #{"1" "3.1" "2"}
 "4" #{"3" "4.1" "4.3" "1" "4.2" "2"}
 "4.1" #{"3" "1" "2"}
 "4.3" #{"3" "4.1" "1" "4.2" "2"}
 "2.2.2" #{"2.1" "2.2.1"}
 "2.1" #{"1"}
 "1.2" #{"1.1"}
 "2.2.1" #{"2.1"}
 "2.2" #{"2.2.2" "2.1" "2.2.1" "1"}
 "2.3" #{"2.1" "2.2" "1"}
 "1" #{"1.2" "1.1"}
 "4.2" #{"3" "4.1" "1" "2"}
 "3.1" #{"1" "2"}
 "2" #{"2.1" "2.2" "2.3" "1"}
 "1.1" #{}}


parsed-yaml ;; list of either ordered maps or an ID

(->> parsed-yaml
     (map (fn [node]
            (if (map? node)
              (first (first node))
              node
              ))))

(defn dep-layer
  "Given a list of dependencies which might be singleton maps with subtasks, list out the top-level taks."
  [dep-tree]
  (->> dep-tree
       (map (fn [node]
              (if (string? node)
                node
                ;; else map
                (first (first node)))
              ))))

(dep-layer parsed-yaml)
;; => ("1" "2" "3" "4")

(dep-layer (get (first parsed-yaml) "1"))
;; => ("1.1" "1.2")

(dep-layer (get (second parsed-yaml) "2"))
;; => ("2.1" "2.2" "2.3")

(defn dep-layers
  "arbitrarily-ordered seq of task dependency levels. It's this layer, with all of the
  layers following."
  [dep-tree]
  (cons
   (dep-layer dep-tree)
   (sequence
    (comp
     (filter map?)
     (mapcat (fn [parent-task]
            (-> parent-task
                vals
                first
                dep-layers
                )
            )))
    dep-tree))
  )

;; Happens to be a preorder traversal. Neat.
(dep-layers parsed-yaml)
'(("1" "2" "3" "4")
  ("1.1" "1.2")
  ("2.1" "2.2" "2.3")
  ("2.2.1" "2.2.2")
  ("3.1")
  ("4.1" "4.2" "4.3"))


(defn layer-dependencies
  "Dependencies from the single layer, where later items depend on previous."
  [tasks]
  (zipmap
   tasks
   (reductions (fn [deps t]
                 (conj deps t))
               #{}
               tasks)))


(layer-dependencies (range 4))
;; => {0 #{}, 1 #{0}, 2 #{0 1}, 3 #{0 1 2}}


(defn prev-deps
  "Dependencies based on previous tasks being dependencies of later tasks at the same layer."
  [task-tree]
  (apply merge-with
         set/union
         (map layer-dependencies (dep-layers task-tree))))


(prev-deps parsed-yaml)
{"3" #{"1" "2"}
 "4" #{"3" "1" "2"}
 "4.1" #{}
 "4.3" #{"4.1" "4.2"}
 "2.2.2" #{"2.2.1"}
 "2.1" #{}
 "1.2" #{"1.1"}
 "2.2.1" #{}
 "2.2" #{"2.1"}
 "2.3" #{"2.1" "2.2"}
 "1" #{}
 "4.2" #{"4.1"}
 "3.1" #{}
 "2" #{"1"}
 "1.1" #{}}


(defn child-deps
  "Subtasks of a task are dependencies of that task's parent."
  [task-tree]
  (apply merge
         (sequence
          (comp
           (filter map?)
           (mapcat (fn [parent-task]
                     (let [parent-id (first (keys parent-task))
                           subtasks (get parent-task parent-id)]
                       (cons
                        {parent-id (set (dep-layer subtasks))}
                        (child-deps subtasks)))
                     )))
          task-tree)))


(child-deps parsed-yaml)
{"1" #{"1.2" "1.1"}
 "2" #{"2.1" "2.2" "2.3"}
 "2.2" #{"2.2.2" "2.2.1"}
 "3" #{"3.1"}
 "4" #{"4.1" "4.3" "4.2"}}


(defn parent-tasks
  "Map from a subtask to its parent task"
  [task-tree]
  (apply merge
         (sequence
          (comp
           (filter map?)
           (mapcat (fn [parent-task]
                     (let [parent-id (first (keys parent-task))
                           subtasks (get parent-task parent-id)]
                       (concat
                        (map (fn [subtask]
                               {subtask parent-id})
                             (dep-layer subtasks))
                        (parent-tasks subtasks))))))
          task-tree)))


(parent-tasks parsed-yaml)
{"4.1" "4"
 "4.3" "4"
 "2.2.2" "2.2"
 "2.1" "2"
 "1.2" "1"
 "2.2.1" "2.2"
 "2.2" "2"
 "2.3" "2"
 "4.2" "4"
 "3.1" "3"
 "1.1" "1"}


(defn all-tasks
  "Seq of all tasks in the given task tree"
  [task-tree]
  (apply concat (dep-layers task-tree)))

(all-tasks parsed-yaml)


(defn parent-deps
  "Child dependencies have all the pre-dependencies of their parents."
  [task-tree]
  (let [prev (prev-deps task-tree)
        parents (parent-tasks task-tree)
        tasks (all-tasks task-tree)]
    (into {}
          (comp
           (filter parents)
           (map (fn [task]
                  [task (get prev (get parents task))]
                  )))
          tasks)))


(parent-deps parsed-yaml)
{"4.1" #{"3" "1" "2"}
 "4.3" #{"3" "1" "2"}
 "2.2.2" #{"2.1"}
 "2.1" #{"1"}
 "1.2" #{}
 "2.2.1" #{"2.1"}
 "2.2" #{"1"}
 "2.3" #{"1"}
 "4.2" #{"3" "1" "2"}
 "3.1" #{"1" "2"}
 "1.1" #{}}


(defn breakdown-deps
  [task-tree]
  (apply merge-with set/union
         ((juxt prev-deps
                parent-deps
                child-deps)
          task-tree)))

(breakdown-deps parsed-yaml)
{"3" #{"1" "3.1" "2"}
 "4" #{"3" "4.1" "4.3" "1" "4.2" "2"}
 "4.1" #{"3" "1" "2"}
 "4.3" #{"3" "4.1" "1" "4.2" "2"}
 "2.2.2" #{"2.1" "2.2.1"}
 "2.1" #{"1"}
 "1.2" #{"1.1"}
 "2.2.1" #{"2.1"}
 "2.2" #{"2.2.2" "2.1" "2.2.1" "1"}
 "2.3" #{"2.1" "2.2" "1"}
 "1" #{"1.2" "1.1"}
 "4.2" #{"3" "4.1" "1" "2"}
 "3.1" #{"1" "2"}
 "2" #{"2.1" "2.2" "2.3" "1"}
 "1.1" #{}}

