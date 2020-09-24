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

(defn filter-from [m v]
  (into {} (vec (for [i v] [i (i m)]))))

(defn transpose [m]
  (vec (for [i (range (count (first m)))]
         (vec (map (fn [v] (v i)) m)))))

(defn transpose-with-sn-added [m]
  (vec (for [i (range (count (first m)))]
         (vec (cons (inc i) (map (fn [v] (v i)) m))))))

(defn get-keys [m]
  (map (fn [m]
         (key m)) m))

(defn get-sequence [m]
  (map (fn [m]
         (val m)) m))

(defn generate-chart-data [m]
  (vec (cons (vec (cons "#" (get-keys m))) (-> m get-sequence transpose-with-sn-added))))

(defn get-chart-data-from [m v]
  (generate-chart-data (filter-from m v)))

(defn get-load-chart-data [m [d l]]
  (vec (cons [(name d) (name l)] (transpose [(d m) (l m)]))))

(defn pad-time-first [m]
  (map (fn [v]
         (vec (conj (map #(% 1) v) (js/Date. (first (first v)))))) m))

(defn generate-seq-chart-data [m]
  (vec (cons (vec (cons "date" (get-keys m))) ((comp pad-time-first transpose-with-sn-added get-sequence) m))))
