(ns oilfield-scada.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.dom :refer [render]]
            [ajax.core :refer [GET POST]]
            [oilfield-scada.websockets :as ws]
            [oilfield-scada.util :refer [generate-chart-data]]))

(def chart-data (atom [["时间" "值"] [0 0]]))

(defonce messages (atom []))

(defn int-value [v]
  (-> v .-target .-value int))

(defn str-value [e]
  (let [v (-> e .-target .-value)]
    (if (nil? v) nil
        (str v))))

(defn from-selected [id]
  (let [e (.getElementById js/document id)]
    (.-text (aget (.-options e) (.-selectedIndex e)))))

(defn message-list []
  [:ul
   (for [[i message] (map-indexed vector @messages)]
     ^{:key i}
     [:li message])])

(defn message-input []
 (let [value (atom nil)]
   (fn []
     [:input.form-control
      {:type :text
       :placeholder "type in a message and press enter"
       :value @value
       :on-change #(reset! value (-> % .-target .-value))
       :on-key-down
       #(when (= (.-keyCode %) 13)
          (ws/send-transit-msg!
           {:message @value})
          (reset! value nil))}])))

(defn add [params result]
  (POST "/dcs/api/plus"
       {:headers {"Accept" "application/transit+json"}
        :params params
        :handler #(reset! result %)}))

(defn get-select-options [param result]
  (GET (str "/dcs/api/query/" param)
       {:headers {"Accept" "application/transit+json"}
        :handler #(reset! result (:result %))}))

(defn get-select-options-via-post [affix params result]
  (POST (str "/dcs/api/query/" affix)
       {:headers {"Accept" "application/transit+json"}
        :params params
        :handler #(reset! result (:result %))}))

(defonce fields (atom nil))

(defonce plants (atom nil))

(defonce wells (atom nil))

(defonce chart-title (atom "采集量"))

(defn wells-list []
  [:div
   [:label.col-md-2 "油井"]
   (vec
    (cons :select.col-md-4
          (cons {:id "well"}
                (if (= 0 (count @wells)) [[:option {:value 0} "请选择"]]
                    (map (fn [{:keys [device_id name]}]
                           [:option {:value device_id} name]) @wells)))))])

(defn plants-list []
  [:div
   [:label.col-md-2 "采油厂"]
   (vec (cons :select.col-md-4
              (cons {:id "plant" :on-change #(get-select-options-via-post "wells" {:id (int-value %)} wells)}
                    (cons [:option "请选择"]
                          (when @plants
                        (map (fn [{:keys [id name]}]
                               [:option {:value id} name]) @plants))))))])

(defn fields-list []
  (get-select-options "fields" fields)
  [:div
   [:label.col-md-2 "油田"]
   (vec (cons :select.col-md-4
              (cons {:id "field" :on-change #(get-select-options-via-post "plants" {:id (int-value %)} plants)}
                    (cons [:option "请选择"]                       
                          (when @fields
                            (map (fn [{:keys [id name]}]
                                   [:option {:value id} name]) @fields))))))])

(defonce attrs (atom nil))

(defn attrs-list []
  (get-select-options "attrs" attrs)
  [:div
   [:label.col-md-2 "属性"]
    (vec (cons :select.col-md-4
          (cons {:name "attr" :id "attr"}
                (cons [:option {:value 0} "请选择"]                       
                      (when @fields
                        (map (fn [{:keys [description name]}]
                               [:option {:value name} description]) @attrs))))))])

(defonce data-grid (atom []))

(defonce start-time (atom nil))

(defonce end-time (atom nil))

(defn get-graph-data [_]
  (let [dev (.-value (.getElementById js/document "well"))
        attr (.-value (.getElementById js/document "attr"))]
    (cond (= dev "0") (js/alert "请选择正确的油井")
          (= attr "0") (js/alert "请选择正确的属性")
          :else (do (get-select-options-via-post "data" {:dev dev :attr attr :start @start-time
                                                         :end (if (nil? @end-time) "null" @end-time) :points 10} data-grid)
                    (reset! chart-title (from-selected "attr"))
                    (reset! chart-data (generate-chart-data @data-grid))))))

(defonce ready? (atom false))

(defonce initialize
  (do (js/google.charts.load (clj->js {:packages ["corechart"]}))
      (js/google.charts.setOnLoadCallback
       (fn google-visualization-loaded []
         (reset! ready? true)))))

(defn data-table [data]
  (cond (map? data) (js/google.visualization.DataTable. (clj->js data))
        (string? data) (js/google.visualization.Query. data)
        (seqable? data) (js/google.visualization.arrayToDataTable (clj->js data))))

(defn draw-chart [chart-type data options]
  [:div
   (if @ready?
     [:div
      {:ref
       (fn [this]
         (when this
           (.draw (new (aget js/google.visualization chart-type) this)
                  (data-table data)
                  (clj->js options))))}]
     [:div "Loading..."])])

(defn home-page []
 [:div.container
  [:div.row
   [:div.col-md-4]
   [:div.col-md-4
    [:h2 "  "]]]
  [:div.row
   [:div.col-md-1]
   [:div.col-md-4
    [fields-list]]
   [:div.col-sm-6
    [plants-list]]]
  [:div.row
   [:div.col-md-1]
   [:div.col-md-4
    [wells-list]]
   [:div.col-md-6
    [attrs-list]]]
  [:div.row
   [:div.col-sm-1]
   [:div.col-sm-4
    [:label.col-md-2 "起始"]
    [:input
     {:id "start-time" :type :datetime-local
      :on-change #(reset! start-time (str (apply str (replace {\T \space} (str-value %))) ":00"))}]]
   [:div.col-sm-6
    [:label.col-md-2 "结束"]
    [:input
     {:id "end-time" :type :datetime-local
      :on-change #(let [v (str-value %)] (if (nil? v) nil (reset! end-time (str (clojure.string/replace v #"T" " " ) ":00"))))}]]]
  [:div.row
   [:div.col-md-12]]
  [:div.row
   [:div.col-md-12]]
  [:div.row
   [:div.col-md-4]
   [:div.col-md-2
    [:button.btn.btn-primary {:on-click get-graph-data} "显示变化曲线"]]
   [:div.col-md-2
    [:button.btn.btn-primary "下载为csv"]]]
  [:div.row
   [:div.col-md-12
    [draw-chart
    "LineChart"
    @chart-data
    {:title @chart-title}]]]])

(defn update-messages! [{:keys [message]}]
  (swap! messages #(vec (take 10 (conj % message)))))

(defn mount-components []
  (render [#'home-page] (.getElementById js/document "app")))

(defn init! []
  #_(ws/make-websocket! (str "ws://" (.-host js/location) "/ws") update-messages!)
  (mount-components))
