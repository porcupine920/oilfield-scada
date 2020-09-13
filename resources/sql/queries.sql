-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, first_name, last_name, email, pass)
VALUES (:id, :first_name, :last_name, :email, :pass)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :id

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id

-- :name create-oil-field! :! :n
-- :doc create a new oilfield record
INSERT INTO oil_field
(name)
VALUES (:name)

-- :name get-oil-field-id :? :1
-- :doc get id of oil field given the name if exist
SELECT id FROM oil_field
WHERE BINARY(name) = BINARY(:name)

-- :name get-last-id :? :1
-- :doc get last insert id
SELECT LAST_INSERT_ID() id

-- :name create-oil-plant! :! :n
-- :doc create a new oilplant record
INSERT INTO oil_plant
(name, field_id)
VALUES (:name, :field_id)

-- :name get-oil-plant-id :? :1
-- :doc get id of oil plant given the name if exist
SELECT id FROM oil_plant
WHERE BINARY(name) = BINARY(:name) AND field_id = :field_id

-- :name create-oil-well! :! :n
-- :doc create a new oil well record
INSERT INTO oil_well
(name, plant_id, device_id)
VALUES (:name, :plant_id, :device_id)

-- :name get-oil-well-id :? :1
-- :doc get id of oil well given the name if exist
SELECT id FROM oil_well
WHERE BINARY(name) = BINARY(:name) AND plant_id = :plant_id

-- :name create-attr! :! :n
-- :doc create a new attribute
INSERT INTO attr
(name)
VALUES (:name)

-- :name get-attr-id :? :1
-- :doc get id of attribute given the name
SELECT id FROM attr
WHERE name = :name

-- :name create-well-attr-value! :! :n
-- :doc create a new well attribute value record
INSERT INTO well_attr_value
(well_id, attr_id, value)
VALUES (:well_id, :attr_id, :value)

-- :name create-error-info! :! :n
-- :doc create a new error info given device id and info
INSERT INTO device_error
(device_id, error, time)
VALUES (:device_id, :error, STR_TO_DATE(:time, '%Y-%m-%d %T'))

-- :name get-attr-value-id :? :1
-- :doc get id of attribute value given attribute and well
SELECT id FROM well_attr_value
WHERE attr_id = :attr_id and well_id = :well_id

