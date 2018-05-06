(ns my-db.setup
  (:require
    [my-db.config :as config]
    [datomic.client.api :as d]))

(def cfg (config/config :dev))
(def client (d/client cfg))
(def mydb-conn (d/connect client {:db-name (config/db-name cfg)}))
