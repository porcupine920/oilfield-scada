(ns oilfield-scada.redis.service
  (:require [taoensso.carmine :as car :refer (wcar)]))

(def redis-conn {:pool {} :spec {:uri "redis://anonymous:my-redis@localhost:6379/"}})

(defmacro wcar* [& body] `(car/wcar redis-conn ~@body))

(defn reset-hashmap-value!
  [dev param]
  (doseq [[k v] param]
    (wcar* (car/hmset dev (name k) v))))

(defn get-latest-value
  [dev]
  (let [res (wcar* (car/hgetall dev))]
    {(keyword dev) (into {} (for [i (range 1 (count res) 2)]
               [(keyword (get res (dec i))) (vec (map #(Float/parseFloat %) (clojure.string/split (get res i) #",")))]))}))
