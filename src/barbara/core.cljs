(ns barbara.core
  (:require [clojure.string :as string]))

(def objc-import (aget js/ObjC "import"))
(def app (js/Application.currentApplication))

(set! (.-includeStandardAdditions app) true)

(objc-import "Cocoa")

(defn get-app [app-name]
  (js/Application app-name))

(defn parse-descriptions [description-map]
  (let [max-key-length (apply max (map count (keys description-map)))
        line-fn (fn [[flag description]]
                  (let [pad-size (- max-key-length (count flag))
                        padding  (string/join (repeat (+ pad-size 2) " "))]
                    (str "    " flag padding description)))]
    (string/join "\n" (map line-fn description-map))))

(defn help-doc
  [& {:keys [description usage flags subcommands]}]
  (string/join
   "\n"
   (cond-> ["Barbara: ⚡CLI for Tunnelblick" ""]
     description (conj description "")
     usage (conj "USAGE:" (str "    " usage) "")
     flags (conj
            "FLAGS:"
            (parse-descriptions (assoc flags "-h, --help" "print help"))
            "")
     subcommands (conj "SUBCOMMANDS:" (parse-descriptions subcommands) ""))))

(defn get-help [help-type]
  (case help-type
    :main (help-doc
           :description "Manage tunnels with a tunnelblick CLI"
           :usage "barbara [SUBCOMMAND]"
           :flags {}
           :subcommands
           {"connect" "connect to a VPN"
            "disconnect" "disconnect from a VPN"
            "launch" "launch tunnelblick"
            "list" "list configurations"
            "quit" "quit tunnelblick"
            "status" "tunnelblick connections status"})
    :status (help-doc
             :description "Get the status of tunnelblick connections"
             :usage "barbara status [FLAGS]"
             :flags {"-f, --format" "choose a format: json, defaults to human readable"})
    :list (help-doc
           :description "list available configurations"
           :usage "barbara list [FLAGS]"
           :flags {"-f, --format" "choose a format: json, defaults to human readable"})
    :run (help-doc
          :description "launch tunnelblick"
          :usage "barbara launch [FLAGS]"
          :flags {})
    :connect (help-doc
              :description "connect to a vpn (or all available)"
              :usage "barbara connect [FLAGS]"
              :flags {"-c, --config, --configuration"
                      "connect to CONFIGURATION. Use \"all\" to connect to all."})
    :disconnect (help-doc
                 :description "disconnect from a vpn (or all available)"
                 :usage "barbara disconnect [FLAGS]"
                 :flags {"-c, --config, --configuration"
                         "connect to CONFIGURATION. Use \"all\" to disconnect from all."})
    :quit (help-doc
           :description "quit tunnelblick"
           :usage "barbara quit [FLAGS]"
           :flags {})



    "HELP!!!!"))

(defn get-tunnelblick []
  (get-app "Tunnelblick"))

(defn log [message]
  (.displayAlert app message))

;; View Tunnelblick
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

(defn get-configuration [tunnelblick configuration-name] (some
   (fn [{:keys [name] :as config}]
     (when (= name configuration-name)
       config))
   (get-configurations tunnelblick)))

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

(defn ^:api status [& {:keys [help format]}]
  (if help
    (get-help :status)
    (let [tunnelblick (get-tunnelblick)
          result (get-configurations tunnelblick)]
      (case format
        "json" (->json result)
        (show-configurations result)))))

(defn ^:api list-configurations [& {:keys [help format]}]
  (if help
    (get-help :list)
    (let [config-names (map :name (get-configurations (get-tunnelblick)))]
      (case format
        "json" (->json config-names)
        (string/join "\n" config-names)))))

;; Control Tunnelblick
(defn ^:api run [& {:keys [help]}]
  (if help
    (get-help :run)
    (do
      (println "launching tunnelblick...")
      (.launch (get-tunnelblick)))))

(defn ^:api quit [& {:keys [help]}]
  (if help
    (get-help :quit)
    (do
      (println "stopping tunnelblick...")
      (.quit (get-tunnelblick)))))

(defn connected? [tunnelblick configuration]
  (-> tunnelblick
      (get-configuration configuration)
      (get :status)
      (= "CONNECTED")))

(defn ^:api connect [& {:keys [help configuration]}]
  (if help
    (get-help :connect)
    (let [app (get-tunnelblick)
          valid-configurations (set (map :name (get-configurations app)))]
      (cond
        (contains? valid-configurations configuration)
        (if (connected? app configuration)
          (str configuration " is already connected")
          (do
            (println "connecting...")
            (.connect app configuration)
            (str "connected to " configuration)))

        (= configuration "all")
        (.connectAll app)

        :else
        (str configuration " is not a valid tunnelblick configuraiton.")))))

(defn ^:api disconnect [& {:keys [help configuration]}]
  (if help
    (get-help :disconnect)
    (let [app (get-tunnelblick)
          valid-configurations (set (map :name (get-configurations app)))]
      (cond
        (contains? valid-configurations configuration)
        (if (connected? app configuration)
          (do
            (println "disconnecting...")
            (.disconnect app configuration)
            (str "disconnected from " configuration))
          (str configuration " is not connected"))

        (= configuration "all")
        (.disconnectAll app)

        :else
        (str configuration " is not a valid tunnelblick configuraiton")))))

;;; CLI
(defn parse-keyword-arg [keyword-arg]
  (case keyword-arg
    ("--configuration" "--config" "-c") :configuration
    ("--format" "-f") :format
    ("--help" "-h") :help
    (keyword keyword-arg)))

(def args
  (let [ns-args (.-arguments js/$.NSProcessInfo.processInfo)
        raw-args (->> (range (.-count ns-args))
                      (map
                       (fn [idx]
                         (.unwrap js/ObjC (.objectAtIndex ns-args idx))))
                      (drop 4))
        command (first raw-args)]
    (if (= (parse-keyword-arg (second raw-args)) :help)
      {:command command :help "--help"}
      (let [options (reduce
                     (fn [acc pair]
                       (let [k (parse-keyword-arg (first pair))
                             v (second pair)]
                         (assoc acc k v)))
                     {}
                     (partition 2 (rest raw-args)))]
        (merge {:command command} options)))))

(defn -main []
  (case (get args :command)
    "--help" (get-help :main)
    "connect" (connect
               :help (get args :help)
               :configuration (get args :configuration))
    "disconnect" (disconnect
                  :help (get args :help)
                  :configuration (get args :configuration))
    "launch" (run :help (get args :help))
    ("list" "ls") (list-configurations
                   :help (get args :help)
                   :format (get args :format))

    "quit" (quit :help (get args :help))
    "status" (status
              :help (get args :help)
              :format (get args :format))
    (get-help :main)))

(-main)
