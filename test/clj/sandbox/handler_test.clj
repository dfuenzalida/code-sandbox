(ns sandbox.handler-test
  (:require
    [clojure.test :refer :all]
    [ring.mock.request :refer :all]
    [sandbox.handler :refer :all]
    [muuntaja.core :as m]
    [mount.core :as mount]))

(defn parse-json [body]
  (m/decode "application/json" body))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'sandbox.config/env
                 #'sandbox.handler/app-routes)
    (f)))

(deftest test-app
  (testing "main route"
    (let [response ((app) (request :get "/"))]
      ;; Redirects to /index.html
      (is (= 302 (:status response)))))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "services"

    (testing "tokens"
      (let [response ((app) (-> (request :post "/api/tokens")
                                (json-body {:username "demo@example.com", :password "dummy"})))]
        (is (= 200 (:status response)))
        (is (= [:token] (keys (m/decode-response-body response)))))

      (let [response ((app) (-> (request :post "/api/tokens")
                                (json-body {:username "invalid-user", :password "dummy"})))]
        (is (= 404 (:status response)))
        (is (= {:error "Username not found"} (m/decode-response-body response))))

      (let [response ((app) (-> (request :post "/api/tokens")
                                (json-body {:no-username "invalid-user", :no-password "dummy"})))]
        (is (= 400 (:status response)))
        #_(is (= {:error "Username not found"} (m/decode-response-body response))))

      )
    ))
