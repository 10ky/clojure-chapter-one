(ns chapter-one.test-data
  (:require [clojure.test :refer :all]
            [util.data-utils :refer [make-user]]
            ))

; Describe the first user with a Map
(def first-user (make-user "rembrandt@gmail.com" "Rembrandt" false ["artist"]))

; Describe the first user with a Map
(def fake-user (make-user "fake-rembrandt@gmail.com" "Rembrandt" false ["artist"]))


