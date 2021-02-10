(ns sandbox.task-service
  (:require [clojure.tools.logging :as log]
            [mount.core :as mount]
            [sandbox.db.core :as db])
  (:import [java.util.concurrent Executors ExecutorService]))

(def executor-service (atom nil))

(defn create-service []
  (let [pool-size 3] ;; TODO expose pool size in config file
    (log/info (format "Creating a fixed pool of %d executors for tasks" pool-size))
    (reset! executor-service (Executors/newFixedThreadPool 3))))

(defn stop-service []
  (when @executor-service
    (log/info "Stopping the executor pool")
    (.shutdown @executor-service)))

(mount/defstate task-runner
  :start (create-service)
  :stop  (stop-service))

;; (.submit @executor-service #(log/info "My task"))

(defn create-runnable-task [task-id]
  (fn []
    (try
      (log/info "Running task" task-id "in the executor service...")
      (db/start-task {:id task-id})
      ;; TODO Create and start a process with the ProcessBuilder
      
      ;; TODO update the task status, return code, stdout, stderr, etc.

      (let [[exit-code stdout stderr] [0 nil nil]]
        (log/info "Ending task" task-id)
        (db/end-task {:id task-id :exit-code exit-code :stdout stdout :stderr stderr}))
      (catch Exception ex (log/warn "Exception:" ex)))))

(defn create-and-run-task [user-id task-id]
  ;; TODO update task in db with creation date and status of QUEUED

  ;; Create a runnable that can be submitted to the executor
  (let [runnable-task (create-runnable-task task-id)]
    (when @executor-service
      (db/update-task-status {:id task-id :state "QUEUED"})
      (.submit @executor-service runnable-task))))


(comment
  (db/get-task {:id 41})
  (db/start-task {:id 41})

  ;; Update the timestamp of a task
  (require '[hugsql.core :as hug])
  (hug/db-run db/*db* "select * from tasks where id = 41")
  (hug/db-run db/*db* "UPDATE tasks SET finished = now() where id = 41")

  ;; ProcessBuilder
  (let [builder (ProcessBuilder. (into-array String ["ls" "-la" "."]))
        process (.start builder)
        exitcode (.waitFor process)]
    (println (slurp (.getInputStream process))))
    
  )
