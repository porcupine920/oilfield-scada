(ns oilfield-scada.routes.home
  (:require
   [oilfield-scada.layout :as layout]
   [oilfield-scada.db.core :as db]
   [clojure.java.io :as io]
   [oilfield-scada.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn about-page [request]
  (layout/render request "about.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/about" {:get about-page}]])

