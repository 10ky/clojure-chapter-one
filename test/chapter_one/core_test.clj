(ns chapter-one.core-test
  (:require [clojure.test :refer :all]
            [chapter-one.test-data :refer [first-user fake-user]]
            [clojure.spec.alpha :as s]
            [chapter-one.test-script :refer :all]
            ))

;;; DATA VALIDATION
(deftest data-validation
  (testing "user data structure validation"
    (are [x y] (= x y)
               true (s/valid? :biz.model/user first-user)
               true (s/valid? :biz.model/user fake-user))
    )
  )

;;; DATOMIC CURD
(deftest db-read-write!
  (def new-entities (write-then-read-test!))
  (testing "Write facts of users to Datomic and read back the same facts"
    (are [x y] (= x y)
               true (attribute-of (:user/id first-user) new-entities)
               true (attribute-of (:user/id fake-user) new-entities)
               )
    )
  (def updated-facts (update-then-read-test!))
  (testing "Update a fact and confirm the update of an entity"
    (is (= true (:user/available? updated-facts))
        )
    )
  (def deleted-facts (delete-datom-test!))
  (testing "Delete a fact and confirm the entity does not contain this fact"
    (is (= nil (:user/available? deleted-facts))
        )
    )
  (def deleted-entity (delete-entity-test!))
  (testing "Delete an entity and confirm the entity no longer exist in latest db"
    (is (= nil (:user/id deleted-entity))
        )
    )
  )