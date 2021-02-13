(ns sandbox.tasks
  (:require [clojure.string :refer [capitalize split]]
            [sandbox.db.core :as db]
            [sandbox.task-service :as ts]
            [sandbox.tokens :as tokens]))

(defn pascal-case
  "Produces a pascalCaseString out of another string"
  [s]
  (when s
    (let [[fst & rst] (split s #"[\W_]")]
      (reduce str fst (map capitalize rst)))))

(defn pascal-keys
  "Given a map with keys on :kebab-case into keys in :pascalCase"
  [m]
  (->> (map (fn [[k v]] [(-> k name pascal-case keyword) v]) m)
       (into {})))

(def task-keys
  [:code :created :finished :exit_code :id :lang :name :started :state :stdout :stderr])

(def task-states
  [:CREATED :QUEUED :RUNNING :COMPLETE])

(defn get-user-tasks [token]
  (when-let [user_id  (->> {:token token} db/get-token :user_id)]
    (let [tasks (db/get-tasks-for-user {:user_id user_id})]
      (->> (map #(select-keys % task-keys) tasks)
           (mapv pascal-keys)))))

;; TODO implement task validation w/schema
(defn create-task [token m]
  (when-let [user_id (->> {:token token} db/get-token :user_id)]
    (let [task (merge m {:user_id user_id :state "CREATED"})
          task-id-map (first (db/create-task! task))]
      ;; run the task
      (ts/create-and-run-task user_id (:id task-id-map))

      ;; return the task
      (-> (db/get-task task-id-map)
          (select-keys task-keys)
          pascal-keys))))

(comment
  (db/get-user {:id 1})
  (def token (tokens/create-token-for-user "demo@example.com"))
  (tokens/user-by-token token)

  (first (get-user-tasks token))

  (create-task token {:code "code1" :lang "lang1" :name "name1"})
  (->> {:token token} db/get-token :user_id)

  (db/get-token {:token token})
  (db/get-tasks-for-user {:user_id 1})

  (db/all-tasks)

  (create-task token {:name "name" :lang "lang" :code "code"})

  )
