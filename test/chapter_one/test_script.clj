(ns chapter-one.test-script
  (:require [clojure.test :refer :all]
            [chapter-one.test-data :refer [first-user fake-user]]
            [chapter-one.core :as core]
            ))

(defn write-then-read-test
  "Insert facts about two users and return all facts of named user"
  []
  (let [user-name (:user/name first-user)
        _ (core/add-users [fake-user])
        db-t1 (core/add-users [first-user])
        ]
    (core/get-user-by-name user-name db-t1))
  )

(defn update-then-read-test
  "Update user's availability and return the user entity"
  []
  (let [user-id (:user/id first-user)
        db-t2 (core/update-availability true user-id)
        ]
    (core/get-user-by-id user-id db-t2))
  )

(defn attribute-of
  "Returns true / false about existence of an id in a vector of datoms"
  [id facts]
  (some (fn [x] (some (fn [y] (= id y)) x)) facts)
  )

(defn delete-datom-test
  "Delete an attribute and returns the entity"
  []
  (let [user-id (:user/id fake-user)
        db-t3 (core/delete-user-availability false user-id)
        ]
    (core/get-user-by-id user-id db-t3))
  )

(defn delete-entity-test
  "Delete an entity and returns a selection of the same entity or nil"
  []
  (let [user-id (:user/id fake-user)
        db-t4 (core/delete-user user-id)
        ]
    (core/get-user-by-id user-id db-t4))
  )