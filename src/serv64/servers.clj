(ns serv64.servers
  (:require [serv64.utils :refer :all]
            [serv64.protocols :refer :all]))

; The client is interacting with the menu
(defrecord MenuServer [state])

; server states:

; main menu
(def main-menu (MenuServer. :main))

; file menu
(def file-menu (MenuServer. :file))

; some menu text
(def main-menu-text "Main Menu\n  f: file menu\n  d: disconnect\n")

(def file-menu-text "File Menu\n  m: main menu\n  l: list files\n")

(def unknown-cmd-text "Unknown command.\n\n")

; some sets of bytes to parse input
(def d-bytes (set (.getBytes "dD" "ascii")))

(def f-bytes (set (.getBytes "fF" "ascii")))

(def m-bytes (set (.getBytes "mM" "ascii")))

(def l-bytes (set (.getBytes "lL" "ascii")))

(def new-line-bytes (set (.getBytes "\r\n" "ascii")))

(extend MenuServer
  ByteServer
  {:recv (fn [menu in-byte timestamp]
           (case (:state menu)
             :main (cond
                     (new-line-bytes in-byte) []
                     (f-bytes in-byte) [[:send file-menu-text] [:push file-menu]]
                     (d-bytes in-byte) [[:send "goodbye\n"] [:disconnect]]
                     :else [[:send unknown-cmd-text] [:send main-menu-text]])
             :file (cond
                     (new-line-bytes in-byte) []
                     (m-bytes in-byte) [[:send main-menu-text] [:pop]]
                     (l-bytes in-byte) [[:send "Available files:\n"] [:list-files]]
                     :else [[:send unknown-cmd-text] [:send file-menu-text]])))})
