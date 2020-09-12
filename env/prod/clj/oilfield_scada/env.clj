(ns oilfield-scada.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[oilfield-scada started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[oilfield-scada has shut down successfully]=-"))
   :middleware identity})
