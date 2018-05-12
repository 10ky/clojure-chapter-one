(ns chapter-one.core
  (:require [biz.model :as model]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [datomic.client.api :as d]
            [my-db.setup :as setup]
            [util.data-utils :refer :all]
            )
  (:gen-class))

;;;; CURD Exercise

;;; Setup a database to store facts about users
;; 1. Establish a connection to database
(def conn setup/mydb-conn)

;; 2. Configure schema of allowed attributes
(defn init-schemas!
  [schemas]
  (try
    (map #(d/transact conn {:tx-data %}) schemas)
    (catch Exception e
      (log/error e "Unable to initialize schemas")))
  )

;;; Writing and reading to database
;; 1. Used for adding new users
(defn add-users!
  "Add a given user to database and returns
   a map of the database at this point in time"
  [users]
  (:db-after (d/transact
               conn
               {:tx-data users})))

;; 2. Used to find an user
(defn get-user-by-name!
  "Get using find"
  [user-name db]
  (d/q '[:find ?e ?a ?v ?tx ?op
         :in $ ?user
         :where [?e :user/name ?user]
         [?e ?a ?v ?tx ?op]]
       db user-name)
  )

(defn get-all-eid!
  "Find all eid that has :user/id attribute"
  [db]
  (flatten (d/q '[:find ?e
                  :in $
                  :where [?e :user/id _]
                  ]
                db))
  )

;; Used to read the datums of an entity by user id
(defn get-user-by-id!
  "Pull a user entity"
  [user-id db]
  (d/pull db '[*] [:user/id user-id])
  )

;;; Update operations
;; 1. Used for update fact about an user
(defn update-availability!
  "Update a user's availability and returns a Map of db"
  [id-key is-available id]
  (:db-after (d/transact
               conn
               {:tx-data [{id-key id :user/available? is-available}]})))


(defn update-availability-by-user-id!
  [is-available user-id]
  (update-availability! :user/id is-available user-id)
  )

(defn update-availability-by-eid!
  [is-available eid]
  (update-availability! :db/id is-available eid)
  )

;; 2. Used for delete the availability attribute of a given user
(defn delete-user-availability!
  "Retract :user/available? setting and document the transaction then returns a Map of db"
  [is-available user-id]
  (:db-after (d/transact
               conn
               {:tx-data [[:db/retract [:user/id user-id] :user/available? is-available]
                          [:db/add "datomic.tx" :db/doc (.concat "remove attribute with " user-id)]]})
    )
  )

;; 3. Used for delete fake Rembrandt
(defn delete-user!
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

(defn curd-exercise!
  "Exercise CURD on Datomic DB"
  [users]
  (let [update! (comp (partial apply-av-pairs update-availability-by-eid!) gen-args)]
    (do (some-> users
                ; Create users
                add-users!
                ; Read users
                get-all-eid!
                ; Update users
                update!
                )
        ; Delete users
        (map #(delete-user! (:user/id %)) users)
        )
    )
  )

(def cli-options
  ;; An option with a required argument
  [["-c" "--count COUNT" "Number of users"
    :default 1
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ;; A non-idempotent option
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(defn -main
  [& args]
  (log/info " CURD exercise with Datomic Client API ")
  (log/info (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)
                  schemas [model/role-schema model/order-schema model/user-schema]
                  schemas-ok? (validate-transactions (init-schemas! schemas))
                  users (gen/sample user-generator (:count options))
                  users-ok? (valid-users users)
                  ]
              (if (and schemas-ok? users-ok?) (curd-exercise! users) nil)
              )
            )
  )