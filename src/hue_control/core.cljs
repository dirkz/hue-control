(ns hue-control.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as dom]
            [goog.events :as events]
            [cljs.core.async :refer [<! put! chan]])
  (:import [goog.net Jsonp XhrIo]
           [goog Uri]))

(defrecord Bridge [ip id mac name])
(defrecord User [name password])

(defn bridge-from-json [xs]
  (.log js/console xs)
  (map #(Bridge. (.-internalipaddress %) (.-id %) (.-macaddress %) (.-name %)) xs))

(defn listen [el type]
  (let [out (chan)]
    (events/listen el type
      (fn [e] (put! out e)))
    out))

(defn load-json [uri]
  (let [out (chan)]
    (.send XhrIo uri
           (fn [e] (let [xhr (.-target e)
                         obj (.getResponseJson xhr)]
                     (put! out (bridge-from-json obj)))))
    out))

(defn load-bridges-url []
  "https://www.meethue.com/api/nupnp")

(defn load-bridges []
  (load-json (load-bridges-url)))

(defn init []
  (go
    (let [bridges (<! (load-bridges))]
      (doseq [b bridges]
        (.log js/console b)))))

(init)

