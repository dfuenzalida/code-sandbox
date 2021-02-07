(ns sandbox.tokens
  (:require [sandbox.db.core :as db])
  (:import [java.util Date UUID]))

(defn create-token-for-user [username]
  (let [token   (str (UUID/randomUUID))]
    (when-let [user_id (-> {:email username} db/get-user-by-email :id)]
      (db/create-token! {:user_id user_id :token token})
      token)))

(defn user-by-token [token] ;; returns [ok? value]
  (when-let [uid (-> {:token token} db/get-token :user_id)]
    (db/get-user {:id uid})))

(comment
  ;; Created by the migration
  (db/get-user {:id 1})
  (db/get-user-by-email {:email "demo@example.com"})
  (def token (create-token-for-user "demo@example.com"))
  (user-by-token token)
  (db/get-token {:token token})
  (db/create-user! {:id 101 :first_name "first"
                    :last_name "last" :email "first@last.com" :pass nil})

  )
