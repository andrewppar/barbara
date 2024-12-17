(ns barbara.core)

(def objc-import (aget js/ObjC "import"))
(def app (js/Application.currentApplication))

(set! (.-includeStandardAdditions app) true)

(objc-import "Cocoa")

(defn get-app [app-name]
  (js/Application app-name))

(defn get-tunnelblick []
  (get-app "Tunnelblick"))

(defn log [message]
  (.displayAlert app message))

(defn quit []
  (.quit (get-tunnelblick)))

(defn status []
  (.getConfigurations (get-tunnelblick)))

(defn run []
  (.launch (get-tunnelblick)))

(def args
  (let [ns-args (.-arguments js/$.NSProcessInfo.processInfo)
        raw-args (->> (range (.-count ns-args))
                      (map
                       (fn [idx]
                         (.unwrap js/ObjC (.objectAtIndex ns-args idx))))
                      (drop 4))
        command (first raw-args)
        options (reduce
                 (fn [acc pair]
                   (let [k (keyword (first pair))
                         v (second pair)]
                     (assoc acc k v)))
                 {}
                 (partition 2 (rest raw-args)))]
    (merge {:command command} options)))


(defn get-arg [arg]
  (get args (keyword (name arg))))

(defn -main []
  (log (str args))
  (log (str (get-arg :command)))
  (case (get-arg :command)
    "launch" (run)
    "quit" (quit)
    "status" (status)
    "that's no good... but kind of cool"))



(-main)
