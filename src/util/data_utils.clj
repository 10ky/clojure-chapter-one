(ns util.data-utils
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    )
  )

(defn validate-user
  "Return a valid user or nil"
  [user]
  (s/valid? :biz.model/user user)
  )

(defn valid-users
  [users]
  (if (every? validate-user users) users nil)
  )

(defn make-user
  [id name available? roles]
  {:user/id         id
   :user/name       name
   :user/available? available?
   :user/roles      roles
   }
  )

(def non-empty-alphanumeric-string-gen
  (gen/such-that not-empty (gen/string-alphanumeric)))

(def email-generator
  (gen/fmap (fn [[a b c]] (str a "@" b "." c))
            (gen/tuple
              non-empty-alphanumeric-string-gen
              non-empty-alphanumeric-string-gen
              non-empty-alphanumeric-string-gen)))

(def user-generator
  (gen/fmap (fn [[a b c d]] (make-user a b c d))
            (gen/tuple
              email-generator
              non-empty-alphanumeric-string-gen
              (gen/boolean)
              (s/gen :user/roles)
              )
            )
  )