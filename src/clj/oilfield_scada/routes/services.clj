(ns oilfield-scada.routes.services
  (:require
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring.coercion :as coercion]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.parameters :as parameters]
    [oilfield-scada.middleware.formats :as formats]
    [oilfield-scada.middleware.exception :as exception]
    [ring.util.http-response :refer :all]
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [oilfield-scada.db.services :refer [create-well-info-from-json! create-device-error-info! get-all-oil-fields get-oil-plants-of-field get-name-devid-of-well get-all-attrs]]
    [oilfield-scada.routes.websockets :refer [notify-clients!]]
    [oilfield-scada.redis.service :refer [get-latest-value]]
    [oilfield-scada.util :refer [uuid upload-data! get-current-time generate-query-resp send-ws-msg!]]))

(defn service-routes [] 
  ["/dcs"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ["/api/plus"
    {:post {:summary "rest api test"
            :parameters {:body {:x int? :y int?}}
            :responses {200 {:body {:result pos-int?}}}
            :handler (fn [{{{:keys[x y]} :body} :parameters}]
                       {:status 200 :body {:result (+ x y)}})}}]

   ["/api/query/data"
    {:post {:summary "query latest value given id of device"
            :parameters {:body {:dev string?}}
            :responses {200 {:body {:result coll?}}}
            :handler (fn [{{{dev :dev} :body} :parameters}]
                       {:status 200 :body {:result (get-latest-value dev)}})}}]

   ["/api/query/data/sequence"
    {:post {:summary "query value according to time, device and attribute"
            :parameters {:body {:start string? :end string? :interval int? :dev string? :attr string? :points int?}}
            :responses {200 {:body {:result vector?}}}
            :handler (fn [{{body :body} :parameters}]
                       {:status 200 :body {:result (generate-query-resp body)}})}}]

   ["/api/query/data/download"
    {:post {:summary "downloads a file"
            :parameters {:body {:data coll?}}
            :swagger {:produces ["text/csv"]}
            :handler (fn [{{{:keys [data]} :body} :parameters}]
                       (let [path (str "/var/tmp/data." (uuid) ".csv")
                             f (future (with-open [w (clojure.java.io/writer path)]
                                         (doseq [row data] (.write w (apply str (conj (interpose "," row) "\n"))))))]
                         (deref f)
                       {:status 200
                        :headers {"Content-Type" "text/csv"}
                        :body (try
                                (-> (str "file://" path)
                                    (io/input-stream))
                                (catch java.io.IOException e
                                           (log/error e))
                                (finally
                                  (future (clojure.java.io/delete-file path))))}))}}]

   ["/api/query/fields"
    {:get {:summary "query ids and names of all oil fields"
           :responses {200 {:body {:result vector?}}}
           :handler (fn [_]
                      {:status 200 :body {:result (get-all-oil-fields)}})}}]

   ["/api/query/plants"
    {:post {:summary "query ids and names of oil plants given field id"
           :parameters {:body {:id int?}}
           :responses {200 {:body {:result vector?}}}
           :handler (fn [{{{:keys [id]} :body} :parameters}]
                      {:status 200 :body {:result (get-oil-plants-of-field id)}})}}]

   ["/api/query/wells"
    {:post {:summary "query names and device ids of oil wells given plant id"
           :parameters {:body {:id int?}}
           :responses {200 {:body {:result vector?}}}
            :handler (fn [{{{:keys [id]} :body} :parameters}]
                      {:status 200 :body {:result (get-name-devid-of-well id)}})}}]

   ["/api/query/attrs"
    {:get {:summary "query names and descriptions of all attributes of well"
           :responses {200 {:body {:result vector?}}}
           :handler (fn [_]
                      {:status 200 :body {:result (get-all-attrs)}})}}]

   ["/daqsystem/register"
    {:post {:summary "device registration"
            :parameters {:body {:device_id string? :type string? :device_type string?
 ;                               :range string? :well_depth string? :pump_depth string?
 ;                               :pump_type string? :rod_parameters string?                               
                                :oil_field string? :oil_plant string? :oil_well string?}}
            :responses {200 {:body {:type string? :device_id string? :device_type string? :result pos-int?}}}
            :handler (fn [{{body :body} :parameters}]
                       (try
                         (create-well-info-from-json! body)
                         {:status 200 :body {:type (:type body) :device_id (:device_id body) :device_type (:device_type body) :result 200}}
                         (catch Exception e
                           (log/error e)
                           {:status 200 :body {:type (:type body) :device_id (:device_id body) :device_type (:device_type body) :result 500}})))}}]

   ["/daqsystem/heartbeat"
    {:post {:summary "heartbeat service"
            :parameters {:body {:device_id string? :type string? :device_type string?
                                :time string? :error string?}}
            :responses {200 {:body {:type string? :device_id string? :device_type string? :result pos-int?}}}
            :handler (fn [{{body :body} :parameters}]
                       {:status 200 :body {:type (:type body) :device_id (:device_id body) :device_type (:device_type body) :result (create-device-error-info! body)}})}}]

   ["/daqsystem/clock"
    {:post {:summary "clock synchronization service"
            :parameters {:body {:device_id string? :type string? :device_type string?}}
            :responses {200 {:body {:type string? :device_id string? :device_type string? :current_date string?}}}
            :handler (fn [{{body :body} :parameters}]
                       {:status 200 :body {:type (:type body) :device_id (:device_id body) :device_type (:device_type body) :current_date (get-current-time)}})}}]

   ["/daqsystem/data"
     {:post {:summary "data acquisition"
             :parameters {:body {:device_id string? :type string? :device_type string? :av string? :ac string?
                                 :ap string? :bv string? :bc string? :bp string? :cv string? :cc string?
                                 :cp string? :load string? :displacement string? :crankpos string? :motorspd string?
                                 :time string?}}
             :responses {200 {:body {:type string?
                                     :device_id string? :device_type string?
                                     :result pos-int?}}}
             :handler (fn [{{body :body} :parameters}]
                        #_(send-ws-msg! body)
                        {:status 200 :body {:type (:type body) :device_id (:device_id body) :device_type (:device_type body) :result (upload-data! body)}})}}]])

