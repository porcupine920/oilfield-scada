(ns oilfield-scada.db.services
  (:require [oilfield-scada.db.core :as db]
            [oilfield-scada.util :refer [dissoc-multiple]]))

(defn to-bytes
  [s]
  (.getBytes s "UTF-8"))

(defn create-oil-field!
  [name]
  (let [id (:id (db/get-oil-field-id {:name (to-bytes name)}))]
    (if (nil? id)
      (do (db/create-oil-field! {:name (to-bytes name)})
          (:id (db/get-last-id)))
      id)))

(defn create-oil-plant!
  [plant field]
  (let [id (:id (db/get-oil-plant-id {:name (to-bytes plant) :field_id (create-oil-field! field)}))]
    (if (nil? id)
      (do (db/create-oil-plant! (assoc {:name (to-bytes plant)} :field_id (create-oil-field! field)))
          (:id (db/get-last-id)))
      id)))

(defn create-oil-well!
  ([well plant field]
   (:id (db/get-oil-well-id {:name well :plant_id (create-oil-plant! plant field)})))
  ([well plant field dev_id]
  (let [id (:id (db/get-oil-well-id {:name (to-bytes well) :plant_id (create-oil-plant! plant field)}))]
    (if (nil? id)
      (do (db/create-oil-well! (assoc {:name (to-bytes well) :device_id dev_id} :plant_id (create-oil-plant! plant field)))
          (:id (db/get-last-id)))
      id))))

(defn create-attr!
  [attr]
  (let [id (:id (db/get-attr-id {:name attr}))]
    (if (nil? id)
      (do (db/create-attr! {:name attr})
          (:id (db/get-last-id)))
      id)))

(defn create-well-attr-value!
  [[attr value] well plant field]
  (let [id (:id (db/get-attr-value-id {:attr_id (create-attr! (name attr)) :well_id (create-oil-well! well plant field)}))]
    (if (nil? id)
      (do (db/create-well-attr-value! {:attr_id (create-attr! (name attr)) :well_id (create-oil-well! well plant field) :value value})
          (:id (db/get-last-id)))
      id)))
  
(defn create-well-attr-value-from-map!
  [m well plant field]
  (if (empty? m) 200
      (do (create-well-attr-value! (first m) well plant field)
          (create-well-attr-value-from-map! (rest m) well plant field))))

(defn create-well-info-from-json!
  [{:keys [device_id oil_field oil_plant oil_well] :as params}]
  (create-oil-well! oil_well oil_plant oil_field device_id))
 #_(create-well-attr-value-from-map! (dissoc-multiple params #{:type :device_id :device_type :oil_field :oil_plant :oil_well}) oil_field oil_plant oil_well)

(defn create-device-error-info!
  [{:keys [device_id error time]}]
  (if (not= 0 (count error)) (db/create-error-info! {:device_id device_id :error error :time time}))
  200)
      
(defn get-all-oil-fields
  "get ids and names of all oil fields"
  []
  (db/get-all-oil-fields))

(defn get-oil-plants-of-field
  [field]
  (db/get-oil-plants-of-field {:id field}))

(defn get-name-devid-of-well
  [plant]
  (db/get-name-dev-id-of-plant {:id plant}))

(defn get-all-attrs
  []
  (db/get-all-attrs))
