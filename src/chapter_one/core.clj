(ns chapter-one.core
  (:require [my-db.setup :as setup]
            [biz.model :as model]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [util.data-utils :refer :all]
            [clojure.tools.logging :as log]
            [datomic.client.api :as d])
  (:gen-class))

;;;; CURD Exercise

;;; Setup a database to store facts about users
;; 1. Establish a connection to database
(def conn setup/mydb-conn)

;; 2. Configure schema of allowed attributes
(defn init-schemas
  [schemas]
  (try
    (map #(d/transact conn {:tx-data %}) schemas)
    (catch Exception e
      (log/error e "Unable to initialize schemas")))
  )

;;; Writing and reading to database
;; 1. Used for adding new users
(defn add-users
  "Add a given user to database and returns
   a map of the database at this point in time"
  [users]
  (:db-after (d/transact
               conn
               {:tx-data users})))

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

(defn get-all-eid
  "Find all eid that has :user/id attribute"
  [db]
  (flatten (d/q '[:find ?e
                  :in $
                  :where [?e :user/id _]
                  ]
                db))
  )

;; Used to read the datums of an entity by user id
(defn get-user-by-id
  "Pull a user entity"
  [user-id db]
  (d/pull db '[*] [:user/id user-id])
  )

;;; Update operations
;; 1. Used for update fact about an user
(defn update-availability
  "Update a user's availability and returns a Map of db"
  [id-key is-available id]
  (:db-after (d/transact
               conn
               {:tx-data [{id-key id :user/available? is-available}]})))


(defn update-availability-by-user-id
  [is-available user-id]
  (update-availability :user/id is-available user-id)
  )

(defn update-availability-by-eid
  [is-available eid]
  (update-availability :db/id is-available eid)
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
  "Pair a random boolean value with each id"
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

(defn curd-exercise
  "Exercise CURD on Datomic DB"
  [users]
  (do (some-> users
              add-users
              ; Extract all entity ids to make later updates
              get-all-eid
              ; Compose boolean eid argument pair for each entity
              gen-args
              ; Make update to all user entities
              ((partial apply-av-pairs update-availability-by-eid))
              )
      (for [id (map :user/id users)] (delete-user id))
      )
  )

(defn -main
  [& args]
  (log/info " CURD exercise with Datomic Client API ")
  (log/info (let [schemas [model/role-schema model/order-schema model/user-schema]
                   schemas-ok? (validate-transactions (init-schemas schemas))
                   users (gen/sample user-generator 10)
                   valid-users? (valid-users users)
                   ]
               (if (and schemas-ok? valid-users?) (curd-exercise users) nil)
               )
             )
  )