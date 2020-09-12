(ns oilfield-scada.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [oilfield-scada.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[oilfield-scada started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[oilfield-scada has shut down successfully]=-"))
   :middleware wrap-dev})
