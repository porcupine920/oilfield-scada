(ns oilfield-scada.util
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.tools.logging :as log]
            [clj-time.format :as f]
            [gniazdo.core :as ws]
            [clj-time.core :refer [to-time-zone time-zone-for-offset]]
            [clj-time.coerce :refer [from-long to-long]]
            [oilfield-scada.redis.service :refer [reset-hashmap-value!]]))

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
    (future (reset-hashmap-value! dev points))
    (into []
          (apply concat (for [[k v] points]
                          (map-indexed (fn [i n]
                                         {:name (apply str (interpose "#" [dev (name k) (inc i)])) :datapoints [[t (Float/parseFloat n)]] :tags {:tagtype "value"}}) (clojure.string/split v #",")))))))

(def server-ip "localhost")

(defn upload-data!
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
  (let [{:keys [start end interval dev attr points] :or {points 600 end nil}} param
        start_time (- (to-long (f/parse (f/formatter "yyyy-MM-dd HH:mm:ss") start)) (* 8 3600000))
        end_time (if (= "null" end) nil (- (to-long (f/parse (f/formatter "yyyy-MM-dd HH:mm:ss") end)) (* 8 3600000)))
        result {:start_absolute start_time :time_zone "Asia/Shanghai"
                :metrics (into [] (for [i (range points)] {:tags {:tagtype ["value"]} :name (apply str (interpose "#" [dev attr (inc i)])) :limit 10000 :aggregators [{:name "first" :sampling {:value interval :unit "minutes"}}]}))}]
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

(defn uuid [] (.toString (java.util.UUID/randomUUID)))

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
  (json/write-str (dissoc-multiple params #{:type :device_id :device_type})))

(defn send-to-ws-chan
  [port chan msg]
  (let [socket (ws/connect (format "ws://localhost:%d/ws" port) :on-error #(log/error "Error occurred: " %))]
    (ws/send-msg socket msg)
    (ws/close socket)))

(defn send-ws-msg!
  [params]
  (let [{:keys [device_id time]} params]
    (send-to-ws-chan 3000 device_id (generate-ws-msg params))))
