(ns hue-control.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as dom]
            [goog.events :as events]
            [cljs.core.async :refer [<! put! chan]])
  (:import [goog.net Jsonp XhrIo]
           [goog Uri]))

(defrecord Bridge [ip id mac name])
(defrecord User [devicetype username])

(defn platform-id []
  (let [nav js/navigator]
    (.replace (str (.-platform nav)) " " "_")))

(defn make-user []
  (User. "hue-control" (platform-id)))

(defn api-url [bridge]
  (str "http://" (:ip bridge) "/api/"))

(defn register-url [bridge user]
  (str (api-url bridge) (:devicetype user)))

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

(defn post-json [uri data]
  (let [out (chan)]
    (.send XhrIo uri
           (fn [e] (let [xhr (.-target e)
                         obj (.getResponseJson xhr)]
                     (put! out (bridge-from-json obj))))
           "POST" data)))

(defn load-bridges-url []
  "https://www.meethue.com/api/nupnp")

(defn load-bridges []
  (load-json (load-bridges-url)))

(defn register-bridge [bridge user]
  (post-json (register-url user bridge) (clj->js user)))

(defn init []
  (.log js/console (make-user))
  (go
    (let [bridges (<! (load-bridges))]
      (doseq [b bridges]
        (.log js/console b)))))

(init)

