(ns oilfield-scada.db.services
  (:require [oilfield-scada.db.core :as db]
            [oilfield-scada.util :refer [dissoc-multiple]]))

(defn create-oil-field!
  [name]
  (let [id (:id (db/get-oil-field-id {:name name}))]
    (if (nil? id)
      (do (db/create-oil-field! {:name name})
          (:id (db/get-last-id)))
      id)))

(defn create-oil-plant!
  [plant field]
  (let [id (:id (db/get-oil-plant-id {:name plant :field_id (create-oil-field! field)}))]
    (if (nil? id)
      (do (db/create-oil-plant! (assoc {:name plant} :field_id (create-oil-field! field)))
          (:id (db/get-last-id)))
      id)))

(defn create-oil-well!
  ([well plant field]
   (:id (db/get-oil-well-id {:name well :plant_id (create-oil-plant! plant field)})))
  ([well plant field dev_id]
  (let [id (:id (db/get-oil-well-id {:name well :plant_id (create-oil-plant! plant field)}))]
    (if (nil? id)
      (do (db/create-oil-well! (assoc {:name well :device_id dev_id} :plant_id (create-oil-plant! plant field)))
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
  (create-oil-well! oil_well oil_plant oil_field device_id)
  (create-well-attr-value-from-map! (dissoc-multiple params #{:type :device_id :device_type :oil_field :oil_plant :oil_well}) oil_field oil_plant oil_well))

