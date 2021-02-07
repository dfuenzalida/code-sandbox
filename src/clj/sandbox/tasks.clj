(ns sandbox.tasks
  (:require [clojure.tools.logging :as log]
            [sandbox.tokens :as tokens]
            [sandbox.db.core :as db]))

(defn get-user-tasks [token]
  (when-let [user_id  (->> {:token token} db/get-token :user_id)]
    (let [db_tasks (->> {:user_id user_id} db/get-tasks-for-user)]
      db_tasks)))

(defn create-task [token m]
  (when-let [user_id (->> {:token token} db/get-token :user_id)]
    (let [task (assoc m :user_id user_id)]
      (db/create-task! task))))

(comment
  (db/get-user {:id 1})
  (def token (tokens/create-token-for-user "demo@example.com"))
  (tokens/user-by-token token)

  (get-user-tasks token)

  (->> {:token token} db/get-token :user_id)

  (db/get-token {:token token})
  (db/get-tasks-for-user {:user_id 1})

  (db/all-tasks)

  (create-task token {:name "name" :lang "lang" :code "code"})

  )
