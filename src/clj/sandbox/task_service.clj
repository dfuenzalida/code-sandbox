(ns sandbox.task-service
  (:require [clojure.tools.logging :as log]
            [mount.core :as mount])
  (:import [java.util.concurrent Executors ExecutorService]))

(def executor-service (atom nil))

(defn create-service []
  (log/info "Creating a fixed pool of 3 executors for tasks")
  (reset! executor-service (Executors/newFixedThreadPool 3)))

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
    (log/info "Running task #" task-id " in the executor service...")
    ;; TODO update the task status, return code, stdout, stderr, etc.
    ))

(defn create-and-run-task [user-id task-id]
  ;; TODO update task in db with creation date and status of QUEUED

  ;; Create a runnable that can be submitted to the executor
  (let [runnable-task (create-runnable-task task-id)]
    (when @executor-service
      (.submit @executor-service runnable-task))))

