(ns serv64.menu
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

(extend MenuServer
  ByteServer
  {:recv (fn [menu msgBytes timestamp]
           (let [msgStr (-> msgBytes
                            (String. "ascii")
                            (.trim)
                            (.toLowerCase))]
             (case (:state menu)
               :main (case msgStr
                       "" []
                       "f" [[:send file-menu-text] [:push file-menu]]
                       "d" [[:send "goodbye\n"] [:disconnect]]
                       [[:send unknown-cmd-text] [:send main-menu-text]])
               :file (case msgStr
                       "" []
                       "m" [[:send main-menu-text] [:pop]]
                       "l" [[:send "Available files:\n"] [:list-files]]
                       [[:send unknown-cmd-text] [:send file-menu-text]]))))})
