(ns barbara.connection
  (:require
   [barbara.args :as args]
   [barbara.configuration :as config]
   [barbara.help :as help]))

(defn connected? [tunnelblick configuration]
  (let [{:keys [state]} (config/fetch tunnelblick configuration)]
    (= state "CONNECTED")))

(defn ^:api connect []
  (let [options (args/options 1)]
    (if (get options :help)
      (help/manpage
       :description "connect to a vpn (or all available)"
       :usage "barbara connect [FLAGS]"
       :flags {"-c, --config, --configuration" "connect to CONFIGURATION"
               "-a, --all" "Connect all configurations"})
      (let [configuration (get options :configuration)
            all? (get options :all)
            tunnelblick (js/Application "Tunnelblick")]
        (cond
          all?
          (.connectAll tunnelblick)

          (config/valid? tunnelblick configuration)
          (if (connected? tunnelblick configuration)
            (str configuration " is already connected")
            (do
              (println "connecting...")
              (.connect tunnelblick configuration)
              (str "connected to " configuration)))

          :else
          (str configuration " is not a valid configuration"))))))

(defn ^:api disconnect []
  (let [options (args/options 1)]
    (if (get options :help)
      (help/manpage
       :description "disconnect from a vpn (or all available)"
       :usage "barbara disconnect [FLAGS]"
       :flags {"-c, --config, --configuration" "disconnect from CONFIGURATION"
               "-a, --all" "disconnect from all configurations"})
      (let [configuration (get options :configuration)
            all? (get options :all)
            tunnelblick (js/Application "Tunnelblick")]
        (cond
          all?
          (.disconnectAll tunnelblick)

          (config/valid? tunnelblick configuration)
          (if (connected? tunnelblick configuration)
            (do
              (println "disconnecting...")
              (.connect tunnelblick configuration)
              (str "disconnected from " configuration))
            (str configuration " is not connected"))

          :else
          (str configuration " is not a valid configuration"))))))
