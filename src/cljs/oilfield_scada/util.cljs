(ns oilfield-scada.util)

(defn pad-zero [v]
  (str (if (< v 10) "0" nil) (str v)))

(defn format-date [date]
  (let [t (js/Date. date)
        year (.getFullYear t)
        month (.getMonth t)
        day (.getDate t)
        hour (.getHours t)
        minute (.getMinutes t)
        sec (.getSeconds t)]
    (str (apply str (interpose "-" [(str year) (pad-zero (inc month)) (pad-zero day)])) " " (apply str (interpose ":" [(pad-zero hour) (pad-zero minute) (pad-zero sec)])))))

(defn transpose [m]
  (vec (for [i (range (count (first m)))]
         (vec (map (fn [v] (v i)) m)))))

(defn get-keys [m]
  (map (fn [m]
         (str "#" ((clojure.string/split (key (first m)) #"#") 2))) m))

(defn get-sequence [m]
  (map (fn [m]
         (val (first m))) m))

(defn pad-time-first [m]
  (map (fn [v]
         (vec (conj (map #(% 1) v) (js/Date. (first (first v)))))) m))

(defn generate-chart-data [m]
  (vec (cons (vec (cons "date" (get-keys m))) ((comp pad-time-first transpose get-sequence) m))))
