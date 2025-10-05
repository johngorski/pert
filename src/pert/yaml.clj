(ns pert.yaml
  (:require
   [clj-yaml.core :as yaml]))


(yaml/parse-string "
- {name: John Smith, age: 33}
- name: Mary Smith
  age: 27
")


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

")






