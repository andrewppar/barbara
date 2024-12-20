(ns barbara.core
  (:require [clojure.string :as string]))

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

(defn fetch-ns-config [tunnelblick idx]
  (.at (.-configurations tunnelblick) idx))

(defn parse-bytes [bytes]
  (str (/ (.parseInt js/Number bytes 10) 1000000)))

(defn parse-configuration [configuration]
  {:name (.name configuration)
   :state (.state configuration)
   :autoconnect (.autoconnect configuration)
   :tx (parse-bytes (.bytesin configuration))
   :rx (parse-bytes (.bytesout configuration))})

(declare ->json)

(defn map->json [cljs-map]
  (str "{"
       (->> cljs-map
            (map (fn [[k v]] (str "\"" (name k) "\":" (->json v))))
            (string/join ","))
       "}"))

(defn coll->json [cljs-collection]
  (str "[" (string/join "," (map ->json cljs-collection)) "]"))

(defn string->json [cljs-string]
  (str "\"" cljs-string "\""))

(defn ->json [object]
  (cond
    (map? object)
    (map->json object)

    (coll? object)
    (coll->json object)

    (string? object)
    (string->json object)

    :else (str object)))

(defn get-configurations [tunnelblick]
  (mapv
   (comp parse-configuration
         (partial fetch-ns-config tunnelblick))
   (range (alength (.configurations tunnelblick)))))

(defn show-configurations [configs]
  (let [column-keys [:name :state :autoconnect :tx :rx]
        columns (reduce
                 (fn [acc col]
                   (assoc acc col (string/upper-case (name col))))
                 {}
                 column-keys)
        with-columns (cons columns configs)
        column->max (reduce
                     (fn [acc column]
                       (let [col-max (+ (apply max (map (comp count column) with-columns)) 2)]
                         (assoc acc column col-max)))
                     {}
                     column-keys)]
    (string/join
     "\n"
     (map
      (fn [config]
        (string/join
         (map
          (fn [col]
            (let [val (get config col)
                  padding (- (get column->max col) (count val))]
              (str val (string/join (repeat padding " ")))))
          column-keys)))
      with-columns))))

(defn status [& {:keys [format]}]
  (let [tunnelblick (get-tunnelblick)
        result (get-configurations tunnelblick)]
    (case format
      "json" (->json result)
      (show-configurations result))))

(defn run []
  (.launch (get-tunnelblick)))

(defn connect [& {:keys [configuration]}])

(defn list-configurations [& {:keys [format]}]
  (let [config-names (map :name (get-configurations (get-tunnelblick)))]
    (case format
      "json" (->json config-names)
      (string/join "\n" config-names))))

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
  (case (get-arg :command)
    "launch" (run)
    "list" (list-configurations :format (or (get-arg :--format) (get-arg :-f)))
    "quit" (quit)
    "connect" (connect :configuration (or (get-arg :--config)
                                          (get-arg :--configuration)
                                          (get-arg :-c)))
    "status" (status :format (or (get-arg :--format) (get-arg :-f)))
    "that's no good... but kind of cool"))



(-main)
