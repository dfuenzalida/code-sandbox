(ns sandbox.tokens-test
  (:require
   [clojure.test :refer :all]
   [sandbox.tokens :refer :all]
   [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'sandbox.config/env
                 #'sandbox.handler/app-routes)
    (f)))

(deftest tokens-tests

  (testing "Create token and retrieving valid user"
    (let [email "demo@example.com"
          token (create-token-for-user email)
          user  (user-by-token token)]
      (is (= email (:email user)))))

  (testing "Attempt to create token for invalid user"
    (let [email (str (java.util.UUID/randomUUID))
          token (create-token-for-user email)]
      (is (nil? token))))

  (testing "Retrieving a user with invalid token"
    (let [token (-> (java.util.Date.) .getTime str)
          user (user-by-token token)]
      (is (nil? user))))

  )
