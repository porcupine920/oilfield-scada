(ns oilfield-scada.util
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.walk :refer [keywordize-keys]]
            [clj-time.format :as f]
            [clj-time.core :refer [to-time-zone time-zone-for-offset]]
            [clj-time.coerce :refer [from-long to-long]]))

(defn dissoc-multiple
  [m s]
  ((fn dissoc-iter
     [m s]
     (if (empty? s) m
         (dissoc-iter (dissoc m (first s)) (rest s)))) m s))

(defn from-data
  [{:keys [time] :as data}]
  (let [t (- (to-long (f/parse (f/formatter "yyyy-MM-dd HH:mm:ss") time)) (* 8 3600000))
        dev (:device_id data)
        points (dissoc-multiple data #{:type :device_id :device_type :time})]
    (into []
          (apply concat (for [[k v] points]
                          (map-indexed (fn [i n]
                                         {:name (apply str (interpose "#" [dev (name k) (inc i)])) :datapoints [[t (Float/parseFloat n)]] :tags {:tagtype "value"}}) (clojure.string/split v #",")))))))

(def server-ip "152.136.103.199")

(defn upload-data
  [param]
  (let [{:keys [status body]}
        (client/post (format "http://%s:8080/api/v1/datapoints" server-ip)
                     {:body (json/write-str (from-data param))})]
    (if (= 204 status) 200
        status)))

(defn get-current-time
  []
  (f/unparse (f/formatter "yyyy-MM-dd HH:mm:ss") (from-long (+ (* 8 3600000) (.getTime (java.util.Date.))))))

(defn generate-request
  [param]
  (let [{:keys [start end dev attr points] :or {points 600 end nil}} param
        start_time (to-long (f/parse (f/formatter "yyyy-MM-dd HH:mm:ss") start))
        end_time (if (empty? end) nil (to-long (f/parse (f/formatter "yyyy-MM-dd HH:mm:ss") end)))
        result {:start_absolute start_time :time_zone "Asia/Shanghai"
                :metrics (into [] (for [i (range points)] {:tags {:tagtype ["value"]} :name (apply str (interpose "#" [dev attr (inc i)])) :limit 10000 :aggregators [{:name "first" :sampling {:value 5 :unit "minutes"}}]}))}]
    (if (nil? end_time) result
        (assoc result :end_absolute end_time))))

(defn query-metric-from
  [ip param]
  (let [{:keys [status body]} (client/post (format "http://%s:8080/api/v1/datapoints/query" ip)
                                           {:body (json/write-str (generate-request param))})]
    (keywordize-keys (json/read-str body))))

(defn result-to-resp
  [{queries :queries}]
  (into []
        (map (fn [x]
               (let [results (:results x)
                     [{:keys [name values]}] results]
                 {name values})) queries)))

(defn generate-query-resp
  [param]
  (result-to-resp (query-metric-from server-ip param)))

(fn [v]
  ((fn merge [v m]
     (if (empty? v) m
         (let [i (first v)
               k (i 0)
               val (i 1)]
           (if (contains? m k)
             (merge (rest v) (update m k conj [val]))
             (merge (rest v) (assoc m k val)))))) v {}))

(defn generate-ws-msg
  [params]
  params)
