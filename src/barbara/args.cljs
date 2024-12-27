(ns barbara.args)

(defn parse-keyword-arg [keyword-arg]
  (case keyword-arg
    ("--configuration" "--config" "-c")
    {:option :configuration :status :complex}

    ("--format" "-f")
    {:option :format :status :complex}

    ("--all" "-a")
    {:option :all :status :simple}

    ("--help" "-h")
    {:option :help :status :simple}

    ("--path" "-p")
    {:option :path :status :complex}

    {:option (keyword keyword-arg) :status :complex}))

(def args
  (let [ns-args (.-arguments js/$.NSProcessInfo.processInfo)]
    (->> (.-count ns-args)
         range
         (map (fn [idx] (.unwrap js/ObjC (.objectAtIndex ns-args idx))))
         (drop 4))))

(defn command []
  (first args))

(defn subcommand []
  (second args))

(defn options [start-index]
  (loop [todo (drop start-index args)
         result {}]
    (if (seq todo)
      (let [{:keys [option status]} (parse-keyword-arg (first todo))]
        (case status
          :simple (recur (rest todo)
                         (assoc result option true))
          :complex (recur (drop 2 todo)
                          (assoc result option (second todo)))))
      result)))
