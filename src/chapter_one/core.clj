(ns chapter-one.core
  (:require [my-db.setup :as setup]
            [biz.model :as model]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.pprint :as pp]
            [clojure.term.colors :refer :all]
            [util.data-utils :refer :all]
            [datomic.client.api :as d])
  (:gen-class))

;;;; CURD Exercise

;;; Setup a database to store facts about users
;;    1. Establish a connection to database
;;    2. Configure schema of allowed attributes

;; 1. Get a connection to mydb
(def conn setup/mydb-conn)

;; 2. Setup user-data schema
(d/transact conn {:tx-data model/role-schema})
(d/transact conn {:tx-data model/order-schema})
(d/transact conn {:tx-data model/user-schema})

;;; Writing and reading to database
;;    1. Create two users one real one fake
;;    2. Read the same fact out of the database

;; 1. Used for adding new users
(defn add-users
  "Add a given user to database and returns
   a map of the database at this point in time"
  [users]
  (:db-after (d/transact
               conn
               {:tx-data users})))

;(defn validate-add-user)
;; 2. Used to find an user
(defn get-user-by-name
  "Get using find"
  [user-name db]
  (d/q '[:find ?e ?a ?v ?tx ?op
         :in $ ?user
         :where [?e :user/name ?user]
         [?e ?a ?v ?tx ?op]]
       db user-name)
  )

(defn get-id-by-name
  "Get using find"
  [user-name db]
  (d/q '[:find ?id
         :in $ ?user
         :where [_ :user/name ?user]
         [_ :user/id ?id]
         ]
       db user-name)
  )

(defn get-all-eid
  "Get using find"
  [db]
  (flatten (d/q '[:find ?e
                  :in $
                  :where [?e :user/id _]
                  ]
                db))
  )

;; Used to read the datums of an entity by user id
(defn get-user-by-id
  "Get using pull"
  [user-id db]
  (d/pull db '[*] [:user/id user-id])
  )

(defn user-id->eid
  "Convert a user id to an entity id"
  [user-id db]
  (d/q '[:find ?e
         :in $ ?user-id
         :where [?e :user/id ?user-id]]
       db user-id)
  )

;;; Update
;;    1. Update a fact about an user
;;    2. Delete that fact from the database
;;    3. Delete the fake Rembrandt entity

;; 1. Used for update fact about an user
(defn update-availability
  "Update a given user's availability and returns a Map of db
   Datomic translates user-id to entity id"
  [is-available user-id]
  (:db-after (d/transact
               conn
               {:tx-data [{:user/id user-id :user/available? is-available}]})))

(defn update-availability'
  "Update a given user's availability and returns a Map of db
   Datomic translates eid to entity eid"
  [is-available eid]
  (:db-after (d/transact
               conn
               {:tx-data [{:db/id eid :user/available? is-available}]})))


(defn bock-update-availability
  ""
  [is-available user-ids]
  (map ((partial update-availability is-available)) user-ids)
  )

;; 2. Used for delete the availability attribute of a given user
(defn delete-user-availability
  "Retract :user/available? setting and document the transaction then returns a Map of db"
  [is-available user-id]
  (:db-after (d/transact
               conn
               {:tx-data [[:db/retract [:user/id user-id] :user/available? is-available]
                          [:db/add "datomic.tx" :db/doc (.concat "remove attribute with " user-id)]]})
    )
  )

;; 3. Used for delete fake Rembrandt
(defn delete-user
  "Retract an entity and document the transaction then returns a Map of db"
  [user-id]
  (:db-after (d/transact
               conn
               {:tx-data [[:db/retractEntity [:user/id user-id]]
                          [:db/add "datomic.tx" :db/doc (.concat "remove entity with " user-id)]]})
    )
  )

(defn gen-args
  "Generate random boolean value for each id"
  [ids]
  (let [vs (gen/sample (s/gen :user/available?) (count ids))]
    (map vector vs ids)
    )
  )

(defn apply-av-pairs
  "Helper function to apply a function to a sequence of argument pairs"
  [f av-pairs]
  (for [av av-pairs] (apply f av))
  )


;;; SAMPLE GENERATORS
;(def image-gen
;  (gen/bind
;    (s/gen (s/tuple pos-int? (s/int-in 1 8) (s/int-in 1 8)))
;    (fn [[max rows cols]]
;      (gen/hash-map
;        :max (s/gen #{max})
;        :data (gen/fmap #(into [] (partition-all cols) %)
;                        (s/gen (s/coll-of (s/int-in 0 (inc max))
;                                          :kind vector?
;                                          :count (* rows cols))))))))
;
;(def role-gen'
;  (gen/bind
;    (s/gen (s/tuple) )
;    (fn [[name]]
;      (gen/hash-map
;        :role/name (s/gen #{name})
;        :user/id (gen/fmap (fn [[a b c]] (str a "@" b "." c))
;                           (gen/tuple
;                             non-empty-alphanumeric-string-gen
;                             non-empty-alphanumeric-string-gen
;                             non-empty-alphanumeric-string-gen))))))

(defn -main
  [& args]
  (println (on-grey (blue " Basic Datomic client functionality ")))
  (println (blue "Exercise CURD on Datomic DB:"))

  ;;; Datomic CURD
  ; CREATE, UPDATE, & DELETE
  (do
    (def users (gen/sample user-generator 100))
    (pp/pprint (if (valid-users users)
                 (do (some-> users
                             add-users
                             ; Extract all entity ids to make later updates
                             get-all-eid
                             ; Compose boolean eid argument pair for each entity
                             gen-args
                             ; Make update to all user entities
                             ((partial apply-av-pairs update-availability'))
                             )
                     (for [id (map :user/id users)] (delete-user id))
                     )
                 nil
                 )
               )
    )
  )