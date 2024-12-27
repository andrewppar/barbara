(ns barbara.tunnelblick
  (:require
   [barbara.args :as args]
   [barbara.help :as help]
   [barbara.view :as view]))

(defn ^:api version []
  (let [options (args/options 1)]
    (if (get options :help)
      (help/manpage
       :descrption "version information"
       :usage "barbara version [FLAGS]"
       :flags {"-f, --format" "choose a format: json, defaults to human readable"})
      (let [version-info (.version (js/Application "Tunnelblick"))]
        (case (get options :format)
          "json" (view/->json version-info)
          (str version-info))))))

(defn ^:api launch []
  (let [options (args/options 1)]
    (if (get options :help)
      (help/manpage
       :description "launch tunnelblick"
       :usage "barbara launch [FLAGS]"
       :flags {})
      (do
        (println "launching tunnelblick...")
        (.launch (js/Application "Tunnelblick"))))))

(defn ^:api quit []
  (let [options (args/options 1)]
    (if (get options :help)
      (help/manpage
       :description "quit tunnelblick"
       :usage "barbara quit [FLAGS]"
       :flags {})
      (do
        (println "stopping tunnelblick...")
        (.quit (js/application "Tunnelblick"))))))
