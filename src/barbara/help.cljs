(ns barbara.help
  (:require [clojure.string :as string]))

(defn parse-descriptions [description-map]
  (let [max-key-length (apply max (map count (keys description-map)))
        line-fn (fn [[flag description]]
                  (let [pad-size (- max-key-length (count flag))
                        padding  (string/join (repeat (+ pad-size 2) " "))]
                    (str "    " flag padding description)))]
    (string/join "\n" (map line-fn description-map))))

(defn manpage
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
