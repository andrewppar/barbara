(ns barbara.core
  (:require
   [barbara.args :as args]
   [barbara.configuration :as config]
   [barbara.connection :as conn]
   [barbara.credentials :as credentials]
   [barbara.help :as help]
   [barbara.tunnelblick :as tunnelblick]))

;;(def objc-import (aget js/ObjC "import"))
;;(def app (js/Application.currentApplication))
;;(set! (.-includeStandardAdditions app) true)
;;(objc-import "Cocoa")

(defn help []
  (help/manpage
   :description "Manage tunnels with a tunnelblick CLI"
   :usage "barbara [SUBCOMMAND]"
   :flags {}
   :subcommands
   {"connect" "connect to a VPN"
    "disconnect" "disconnect from a VPN"
    "launch" "launch tunnelblick"
    "list, ls" "list configurations"
    "quit" "quit tunnelblick"
    "status" "tunnelblick connections status"}))

(defn -main []
  (case (args/command)
    "--help" (help)
    "connect" (conn/connect)
    "disconnect" (conn/disconnect)
    "install" (config/install)
    "launch" (tunnelblick/launch)
    ("list" "ls") (config/names)
    "quit" (tunnelblick/quit)
    "credentials" (credentials/dispatch)
    "status" (config/status)
    "version" (tunnelblick/version)
    (help)))

(-main)
