(ns sandbox.task-service
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [mount.core :as mount]
            [sandbox.config :refer [env]]
            [sandbox.db.core :as db])
  (:import [java.nio.file Files Paths]
           java.util.concurrent.Executors))

(defn timeout []
  (or (-> env :timeout) 10))

(defn pool-size []
  (or (-> env :pool-size) 3))

(def executor-service (atom nil))

(defn create-service []
  (log/infof "Creating a fixed pool of %d executors for tasks" (pool-size))
  (reset! executor-service (Executors/newFixedThreadPool (pool-size))))

(defn stop-service []
  (when @executor-service
    (log/info "Stopping the executor pool")
    (.shutdown @executor-service)))

(mount/defstate task-runner
  :start (create-service)
  :stop  (stop-service))

(defn task-dir [task-id]
  (format "/tmp/code-sandbox/%s" task-id))

(defn write-task-script! [task-id task-code]
  (try
    (let [dir  (task-dir task-id)
          task-file (format "%s/script.groovy" dir)]
      (log/infof "Creating directory for task #%s on %s" task-id dir)
      (io/make-parents task-file)
      (log/infof "Writing script for task #%s" task-id)
      (spit task-file task-code))
    (catch Exception ex (log/warn "Exception:" ex))))

(defn process-args [task-id lang]
  (let [volume (format "%s:/groovyScripts:ro" (task-dir task-id))]
    ["docker" "run" "--rm" "--network" "host" "-v" volume
     "-w" "/groovyScripts" "groovy" "timeout" (str (timeout))
     lang "/groovyScripts/script.groovy"]))

(defn delete-task-script! [task-id]
  (try
    (let [dir (task-dir task-id)
          dir-path (Paths/get dir (into-array String []))
          script (format "%s/script.groovy" dir)
          script-path (Paths/get script (into-array String []))]
      (log/infof "Deleting script for task #%s" task-id)
      (Files/delete script-path)
      (log/infof "Deleting directory for task #%s" task-id)
      (Files/delete dir-path))
    (catch Exception ex (log/warn ex))))

;; (delete-task-script! 8)

(defn run-process [task-id lang]
  (let [builder-args (process-args task-id lang)
        _ (log/infof "Starting process for task #%s with timeout of %s seconds"
                     task-id timeout)
        builder (ProcessBuilder. (into-array String builder-args))
        process (.start builder)
        exit-code (.waitFor process)
        _ (log/infof "Process for task #%s finished" task-id)
        stderr (slurp (.getErrorStream process))
        stdout (slurp (.getInputStream process))]
    {:id task-id :exit-code exit-code :stdout stdout :stderr stderr}))

(defn create-runnable-task [task-id]
  (fn []
    (try
      (let [{:keys [lang code]} (db/get-task {:id task-id})]
        (log/info "Running task" task-id "in the executor service...")

        ;; Prepare
        (write-task-script! task-id code)
        (db/start-task {:id task-id})

        ;; Run
        (let [process-result (run-process task-id lang)]
          (db/complete-task process-result))

        ;; Cleanup
        (delete-task-script! task-id))
      (catch Exception ex (log/warn "Exception when running task:" ex)))))

;; extracted to separate function for simpler mocking
(defn submit-task [runnable-task]
  (when @executor-service
    (.submit @executor-service runnable-task)))

(defn create-and-run-task [user-id task-id]
  ;; Create a runnable that can be submitted to the executor
  (let [runnable-task (create-runnable-task task-id)]
    (when @executor-service
      (submit-task runnable-task)
      ;; Update task in db with creation date and status of QUEUED
      (db/update-task-status {:id task-id :state "QUEUED"}))))

(comment
  (db/get-task {:id 7})
  (db/start-task {:id 41})

  ;; Update the timestamp of a task
  (require '[hugsql.core :as hug])
  (hug/db-run db/*db* "select * from tasks where id = 41")
  (hug/db-run db/*db* "delete from tasks where id < 9")
  (hug/db-run db/*db* "UPDATE tasks SET finished = now() where id = 41")

  ;; ProcessBuilder
  (let [builder (ProcessBuilder. (into-array String ["ls" "-la" "."]))
        process (.start builder)
        exitcode (.waitFor process)]
    (println (slurp (.getInputStream process))))
    
  )
