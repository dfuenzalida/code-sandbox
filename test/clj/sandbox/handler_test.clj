(ns sandbox.handler-test
  (:require [clojure.test :refer :all]
            [mount.core :as mount]
            [muuntaja.core :as m]
            [ring.mock.request :refer :all]
            [sandbox.handler :refer :all]
            [sandbox.tasks :as tasks]
            [sandbox.tokens :as tokens]
            [struct.core :as st])
  (:import java.util.UUID))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'sandbox.config/env
                 #'sandbox.handler/app-routes)
    (f)))

(deftest default-routes
  (testing "main route"
    (let [response ((app) (request :get "/"))]
      ;; Redirects to /index.html
      (is (= 302 (:status response)))))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response))))))

(deftest service-routes

    (testing "Tokens"
      (let [response ((app) (-> (request :post "/api/tokens")
                                (json-body {:username "demo@example.com", :password "dummy"})))]
        (is (= 200 (:status response)))
        (is (= [:token] (keys (m/decode-response-body response)))))

      (let [response ((app) (-> (request :post "/api/tokens")
                                (json-body {:username "invalid-user", :password "dummy"})))]
        (is (= 404 (:status response)))
        ;; #_(is (= {:error "Username not found"} (m/decode-response-body response)))
        )

      (let [response ((app) (-> (request :post "/api/tokens")
                                (json-body {:no-username "invalid-user"
                                            :no-password "dummy"})))]
        (is (= 400 (:status response)))
        ;; #_(is (= {:error "Username not found"} (m/decode-response-body response)))
        ))

    (testing "Tasks"
      (testing "Attemps to retrieve or create tasks without a valid token should fail"

        ;; Get user tasks without providing a token
        (let [response ((app) (request :get "/api/tasks"))]
          (is (= 403 (:status response))))

        ;; Create a task without providing a token
        (let [response ((app) (-> (request :post "/api/tasks")
                                  (json-body {:name "name" :code "println(1)"
                                              :lang "groovy"})))]
          (is (= 403 (:status response)))))

      (testing "Attempts to retrieve or create tasks with invalid tokens should fail"
        (let [auth "Bearer INVALID-TOKEN-HERE"
              response ((app) (-> (request :post "/api/tasks")
                                  (update-in [:headers] assoc "authorization" auth)
                                  (json-body {:name "name" :code "println(1)"
                                              :lang "groovy"})))]
          (is (= 404 (:status response)))))

      (testing "Attempts to create tasks with invalid lang or code should fail"
        (let [token (tokens/create-token-for-user "demo@example.com")
              auth (str "Bearer " token)
              long-code (reduce str "println(1)\n" (repeat (* 65 1024) "\n"))
              payload {:name "name" :code long-code :lang "DUMMY"}
              response ((app) (-> (request :post "/api/tasks")
                                  (update-in [:headers] assoc "authorization" auth)
                                  (json-body payload)))
              [expected _] (st/validate payload tasks/task-schema)]

          ;; look for errors for both code and lang properties
          (is (= #{:code :lang} (set (keys expected))))

          ;; validate contents of the response
          (is (= {:error expected}
                 (m/decode-response-body response)))))

      (testing "Creating tasks and retrieving them"
        ;; Prevent actual tasks to be submitted to the service
        (with-redefs [sandbox.task-service/submit-task (fn [& _] nil)]
          (let [token (tokens/create-token-for-user "demo@example.com")
                auth (str "Bearer " token)
                task-names (map (fn [_] (str (UUID/randomUUID))) (range 10))
                resps (for [tname task-names]
                        ((app) (-> (request :post "/api/tasks")
                                   (update-in [:headers] assoc "authorization" auth)
                                   (json-body {:name tname :lang "groovy"
                                               :code "println(1)"}))))
                tasks-created (map m/decode-response-body resps)
                resp-names (set (map :name tasks-created))]

            ;; The name of every task requested should exist on the set of the response
            (is (every? resp-names task-names)))))
      ))
