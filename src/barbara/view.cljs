(ns barbara.view
  (:require [clojure.string :as string]))

;; this namespace exists to avoid dependencies
;; that makes packaging easier

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
