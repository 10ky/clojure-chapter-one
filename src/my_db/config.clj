(ns my-db.config
  (:require [aero.core :as aero])
  )

(defn config! [profile]
  (aero/read-config (clojure.java.io/resource "config.edn") {:profile profile}))

(defn db-name [config]
  (get-in config [:db-name]))
