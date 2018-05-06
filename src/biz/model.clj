(ns biz.model
  (:require [clojure.spec.alpha :as s]
            [provisdom.spectomic.core :as spectomic]
            ))

(defn email-address? [st]
  (boolean (re-matches #".+\@.+\..+" st)))

(s/def :user/id (s/and string? email-address?))
(s/def :user/name string?)
(s/def :user/available? boolean?)

(s/def :role/name string?)
(s/def :user/roles (s/coll-of :role/name))

(s/def :order/id uuid?)
(s/def :user/order (s/keys :req [:user/id :order/id]))
(s/def :user/orders (s/coll-of :user/order))

(s/def ::user (s/keys :req [:user/id :user/name :user/available? :user/roles]))

(def user-schema [[:user/id {:db/valueType :db.type/string :db/unique :db.unique/identity
                             :db/index     true :db/cardinality :db.cardinality/one}]
                  :user/name
                  :user/available?
                  :user/roles
                  ])
(def role-schema [:role/name {:db/valueType :db.type/string :db/unique :db.unique/identity
                              :db/index     true :db/cardinality :db.cardinality/one}])
(def order-schema [[:order/id {:db/unique :db.unique/identity :db/index true}]])

(defmacro defschema
  [name]
  `(def ~name (spectomic/datomic-schema user-schema)))
; Define schema at compile time
(defschema role-schema)
(defschema order-schema)
(defschema user-schema)