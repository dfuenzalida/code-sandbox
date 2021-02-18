(ns sandbox.handler
  (:require [mount.core :as mount]
            [reitit.ring :as ring]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [sandbox.env :refer [defaults]]
            [sandbox.middleware :as middleware]
            [sandbox.routes.services :refer [service-routes]]))

(mount/defstate init-app
  :start ((or (:init defaults) (fn [])))
  :stop  ((or (:stop defaults) (fn []))))

(mount/defstate app-routes
  :start
  (ring/ring-handler
   (ring/router
    [
     ["/swag" {:get
               {:handler (constantly {:status 301 :headers {"Location" "/api/api-docs/index.html"}}) }}]
     (service-routes)])
   (ring/routes
    (ring/create-resource-handler
     {:path "/"})
    (wrap-content-type (constantly nil))
    (ring/create-default-handler))))

(defn app []
  (middleware/wrap-base #'app-routes))
