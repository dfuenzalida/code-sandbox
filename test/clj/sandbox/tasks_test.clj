(ns sandbox.tasks-test
  (:require [clojure.test :refer :all]
            [mount.core :as mount]
            [sandbox.task-service :as task-service]
            [sandbox.tasks :refer :all]
            [sandbox.tokens :as tokens])
  (:import [java.util UUID]))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'sandbox.config/env
                 #'sandbox.task-service/task-runner
                 #'sandbox.handler/app-routes)
    (f)))

(deftest util-functions-tests
  (testing "Pascal case"
    (is (nil? (pascal-case nil)))
    (is (= "" (pascal-case "")))
    (is (= "wordsSeparatedByThings" (pascal-case "words separated_by-things"))))

  (testing "Pascal keys"
    (is (= {} (pascal-keys {})))
    (let [expected {:oneTwoThree 123, :fourFive 45}
          found    (pascal-keys {:one-two_three 123 :four-five 45})]
      (is (= found expected)))))

(deftest task-creation-and-retrieval-tests

  (testing "Task creation"
    ;; Mocks
    (let [task-was-submitted (atom false)
          task-submit-fn (fn [& _] (reset! task-was-submitted true))]
      (with-redefs [sandbox.task-service/submit-task task-submit-fn]

        ;; Test
        (let [email "demo@example.com"
              token (tokens/create-token-for-user email)
              my-task {:name "test task" :code "println(1)" :lang "groovy"}
              created-task (create-task token my-task)]
          (is (= my-task (select-keys created-task [:name :code :lang])))
          (is (= (set (keys created-task))
                 (set (map (comp keyword pascal-case name) task-keys))))
          (is (true? @task-was-submitted))))))

  (testing "Attempt to create tasks without valid tokens"
    (is (nil? (create-task nil {:name "ok" :code "println(1)" :lang "groovy"})))
    (is (nil? (create-task "dummy" {:name "ok" :code "println(1)" :lang "groovy"}))))

  (testing "Task creation and retrieval"

    ;; Mocks
    (let [num-tasks 10 ;; Create this many tasks for testing
          num-tasks-submitted (atom 0)
          task-submit-fn (fn [& _] (swap! num-tasks-submitted inc))]
      (with-redefs [sandbox.task-service/submit-task task-submit-fn]

        ;; Test
        (let [email "demo@example.com"
              token (tokens/create-token-for-user email)
              task-names (mapv (fn [_] (str (UUID/randomUUID))) (range num-tasks))
              test-tasks (mapv
                          (fn [n] {:name n :lang "groovy" :code "println(1)"})
                          task-names)]

          ;; Create test tasks
          (doseq [task test-tasks]
            (create-task token task))

          ;; Retrieve and compare the names of tasks
          (let [user-tasks (get-user-tasks token)
                user-task-names-set (set (map :name user-tasks))]
            (is (= num-tasks @num-tasks-submitted))

            ;; Every task created should be in the set of names retrieved from db
            (is (every? user-task-names-set task-names)))))))
  )
