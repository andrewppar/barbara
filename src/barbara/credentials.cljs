(ns barbara.credentials
  (:require
   [barbara.args :as args]
   [barbara.configuration :as config]
   [barbara.help :as help]
   [clojure.string :as string]))

(defn credentials-clear []
  (let [options (args/options 2)]
    (if (get options :help)
      (help/manpage
       :description "remove tunnelblick all credentials from configuration"
       :usage "barbara credentials remove [FLAGS]"
       :flags {"-c, --config, --configuration"
               "Remove CONFIGURATION."})

      (let [tunnelblick (js/Application "Tunnelblick")
            config (get options :configuration)
            #_#_all? (get options :all)]
        (if (config/valid? tunnelblick config)
          (.deleteAllCredentialsFor tunnelblick config)
          (str config " is not a valid tunnelblick configuration"))))))

(defn credentials-set []
  (let [{:keys [help configuration password username]} (args/options 2)]
    (if help
      (help/manpage
       :description "set credentials for configuration"
       :usage "barbara credentials set [FLAGS]"
       :flags {"-c, --config, --configuration"
               "Set credentials for configuration (must be specified)"
               "-w, --password" "set password"
               "-u, --username" "set username"})
      (cond
        (not configuration)
        (println
         (string/join
          "\n"
          ["Cannot set credentials without a configuration."
           "Hint: Use one of -c, --config, or --configuration"
           "Use --help for more details."]))

        (not (or password username))
        (string/join
         "\n"
         ["One of --username or --password must be specified."])

        :else
        (let [tunnelblick (js/Application "Tunnelblick")]
          (when username
            (.saveUsername tunnelblick username (clj->js {:for configuration})))
          (when password
            (.savePassword tunnelblick password (clj->js {:for configuration}))))))))



(defn ^:api dispatch []
  (case (args/subcommand)
    "clear" (credentials-clear)
    "set" (credentials-set)
    (help/manpage
     :description "manage tunnelblick credentials"
     :useage "barbara credentials SUBCOMMAND [FLAGS]"
     :subcommands {"set" "set credentials for a profile"
                   "clear" "clear credentials for a configuration"}
     :flags {"-c, --config, --configuration"
             "operate on the credentials for CONFIGURATION"})))
