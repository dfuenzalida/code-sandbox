(ns sandbox.middleware.formats
  (:require
    [luminus-transit.time :as time]
    [muuntaja.core :as m]))

;; TODO delete namespace

;; (def instance
;;   (m/create m/default-options))

;; (def instance-old
;;   (m/create
;;     (-> m/default-options
;;         (update-in
;;           [:formats "application/transit+json" :decoder-opts]
;;           (partial merge time/time-deserialization-handlers))
;;         (update-in
;;           [:formats "application/transit+json" :encoder-opts]
;;           (partial merge time/time-serialization-handlers)))))
