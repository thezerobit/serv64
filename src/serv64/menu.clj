(ns serv64.menu
  (:use serv64.utils serv64.protocols))

; The client is interacting with the menu
(defrecord MenuServer [state])

; server states:

; main menu
(def main-menu (MenuServer. :main))

; file menu
(def file-menu (MenuServer. :file))

(def select-file (MenuServer. :select))

; some menu text
(def main-menu-text "Main Menu\n  f: file menu\n  d: disconnect\n")

(def file-menu-text "File Menu\n  m: main menu\n  l: list files\n  x: download file with xmodem\n")

(def unknown-cmd-text "Unknown command.\n\n")

(extend MenuServer
  ByteServer
  {:activate (fn [menu timestamp]
               (case (:state menu)
                 :main [[:send main-menu-text]]
                 :file [[:send file-menu-text]]
                 :select [[:send "Enter file number:"]]))
   :recv (fn [menu msgBytes timestamp]
           (let [msgStr (-> msgBytes
                            (String. "ascii")
                            (.trim)
                            (.toLowerCase))]
             (case (:state menu)
               :main (case msgStr
                       "" []
                       "f" [[:push file-menu]]
                       "d" [[:send "goodbye\n"] [:disconnect]]
                       [[:send unknown-cmd-text] [:send main-menu-text]])
               :file (case msgStr
                       "" []
                       "m" [[:pop]]
                       "l" [[:send "Available files:\n"] [:list-files]]
                       "x" [[:replace select-file]]
                       [[:send unknown-cmd-text] [:send file-menu-text]])
               :select (let [index (try (Long/parseLong msgStr)
                                      (catch Exception e nil))]
                         (cond
                           (or (nil? index) (< index 1)) [[:send "Invalid input.\n"] [:pop]]
                           :else [[:xmodem index]])))))})
