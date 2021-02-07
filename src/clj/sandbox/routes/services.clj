(ns sandbox.routes.services
  (:require
   [muuntaja.core :as m]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   ;; [sandbox.middleware.formats :as formats]
   [sandbox.tokens :as tokens]
   [sandbox.tasks :as tasks]
   [ring.util.http-response :refer :all]
   [clojure.java.io :as io]))

(defn service-routes []
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja (m/create) ;; TODO remove formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 coercion/coerce-exceptions-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url "/api/swagger.json"
              :config {:validator-url nil}})}]]

   ["/ping"
    {:get (constantly (ok {:message "pong"}))}]

   ["/tokens"
    {:post {:summary "Creates a new token for using the other APIs"
            :parameters {:body {:username string? :password string?}}
            :handler (fn [{{{:keys [username password]} :body} :parameters}]
                       (if-let [token (tokens/create-token-for-user username)]
                         {:status 200 :body {:token token}}
                         {:status 404 :body {:error "Username not found"}}))}
     }]

   ["/tasks"
    {:get {:summary "Return all tasks for a user identified by token"
           :handler (fn [{{:strs [authorization]} :headers}]
                      (if authorization
                        (let [token (last (clojure.string/split authorization #" "))
                              tasks (tasks/get-user-tasks token)]
                          (if tasks
                            {:status 200 :body {:tasks tasks}}
                            {:status 404 :body {:error "Invalid token"}}))
                        {:status 403 :body {:error "Token missing"}}))}
     }]

   ])
