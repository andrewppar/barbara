(ns barbara.credentials
  (:require
   [barbara.args :as args]
   [barbara.configuration :as config]
   [barbara.help :as help]))

(defn credentials-clear []
  (let [options (args/options 2)]
    (if (get options :help)
      (help/manpage
       :description "remove tunnelblick all credentials from configuration"
       :usage "barbara remove [FLAGS]"
       :flags {"-c, --config, --configuration"
               "Remove CONFIGURATION."})

      (let [tunnelblick (js/Application "Tunnelblick")
            config (get options :configuration)
            #_#_all? (get options :all)]
        (if (config/valid? tunnelblick config)
          (.deleteAllCredentialsFor tunnelblick config)
          (str config " is not a valid tunnelblick configuration"))))))

(defn ^:api dispatch []
  (case (args/subcommand)
    "clear" (credentials-clear)
    (help/manpage
     :description "manage tunnelblick credentials"
     :useage "barbara credentials SUBCOMMAND [FLAGS]"
     :subcommands {#_#_"save" "save credentials for a profile"
                   "clear" "clear credentials for a configuration"}
     :flags {"-c, --config, --configuration"
             "operate on the credentials for CONFIGURATION"})))
