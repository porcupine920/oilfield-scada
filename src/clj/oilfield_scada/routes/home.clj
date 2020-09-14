(ns oilfield-scada.routes.home
  (:require
   [oilfield-scada.layout :as layout]
   [oilfield-scada.db.core :as db]
   [clojure.java.io :as io]
   [oilfield-scada.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])

