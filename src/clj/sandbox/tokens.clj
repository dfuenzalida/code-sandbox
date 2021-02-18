(ns sandbox.tokens
  (:require [clojure.tools.logging :as log]
            [sandbox.db.core :as db]
            [struct.core :as st])
  (:import java.util.UUID))

(def token-schema
  [[:username st/required st/string]
   [:password st/required st/string]])

(defn create-token-for-user [username]
  (log/info "Creating token for:" username)
  (let [token (str (UUID/randomUUID))]
    (when-let [user_id (-> {:email username} db/get-user-by-email :id)]
      (db/create-token! {:user_id user_id :token token})
      (log/info "Token created successfully")
      token)))

(defn user-by-token [token]
  (log/info "Looking for token" token)
  (when-let [uid (-> {:token token} db/get-token :user_id)]
    (log/info "User found")
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
