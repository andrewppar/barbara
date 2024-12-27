(ns barbara.configuration
  (:require
   [barbara.args :as args]
   [barbara.help :as help]
   [barbara.view :as view]
   [clojure.string :as string]))

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

(defn all [tunnelblick]
  (mapv
   (comp parse-configuration
         (partial fetch-ns-config tunnelblick))
   (range (alength (.configurations tunnelblick)))))

(defn valid? [tunnelblick configuration-name]
  (contains? (set (map :name (all tunnelblick))) configuration-name))

(defn fetch [tunnelblick configuration-name]
  (some
   (fn [{:keys [name] :as config}]
     (when (= name configuration-name)
       config))
   (all tunnelblick)))

(defn ^:private max-column-val [column offset maps]
  (+ (apply max (map (comp count column) maps)) offset))

(defn ^:private show-config [column->max-val config-keys config]
  (string/join
   (map
    (fn [column]
      (let [val (get config column)
            padding (- (get column->max-val column) (count val))]
        (str val (string/join (repeat padding " ")))))
    config-keys)))

(defn show [configurations]
  (let [column-keys [:name :state :autoconnect :tx :rx]
        columns (->> column-keys
                     (map (juxt identity (comp string/upper-case name)))
                     (into {}))
        with-columns (cons columns configurations)
        column->max (reduce
                     (fn [acc column]
                       (assoc acc column (max-column-val column 2 with-columns)))
                     {}
                     column-keys)]
    (->> with-columns
         (map (partial show-config column->max column-keys))
         (string/join "\n"))))

(defn ^:api status []
  (let [options (args/options 1)]
    (if (get options :help)
      (help/manpage
             :description "Get the status of tunnelblick connections"
             :usage "barbara status [FLAGS]"
             :flags {"-f, --format" "choose a format: json, defaults to human readable"})
      (let [tunnelblick (js/Application "Tunnelblick")
            result (all tunnelblick)]
        (case (get options :format)
          "json" (view/->json result)
          (show result))))))

(defn ^:api names []
  (let [options (args/options 1)]
    (if (get options :help)
      (help/manpage
       :description "list available configurations"
       :usage "barbara list [FLAGS]"
       :flags {"-f, --format" "choose a format: json, defaults to human readable"})
      (let [config-names (map :name (all (js/Application "Tunnelblick")))]
        (case (get options :format)
          "json" (view/->json config-names)
          (string/join "\n" config-names))))))

(defn ^:api install []
  (let [options (args/options 1)]
    (if (get options :help)
      (help/manpage
       :description "Install profile at PATH."
       :usage "barbara install [FLAGS]"
       :flags {"-p, --path" "path of .ovpn profile to install"})
      (if-let [path (get options :path)]
        (let [finder (js/Application "Finder")
              tunnelblick (js/Application "Tunnelblick")]
          (set! (.-includeStandardAdditions tunnelblick) true)
          (.open finder
                 (clj->js [(js/Path. path)])
                 (clj->js {:using (.pathTo tunnelblick)})))
        (str "no path specified for installation")))))
