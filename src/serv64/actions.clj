(ns serv64.actions
  (:require [serv64.utils :refer :all])
  (:import
    [java.io File FileFilter]))

; the protocol that represents a functional interface for
; a stateful server
(defprotocol ByteServer
  (recv [state in-byte timestamp] "returns an Action"))


; action field is either :ignore :update :disconnect
(defrecord Action [action state output])

; states

; The client is interacting with the menu
(defrecord Menu [state])

; The client has disconnected, or will be soon.
(defrecord Disconnect [reason])


; some menu text
(def main-menu-text "Main Menu\n  f: file menu\n  d: disconnect\n")

(def file-menu-text "File Menu\n  m: main menu\n  l: list files\n")

(def unknown-cmd-text "Unknown command.\n\n")


(defn get-files []
  "returns an array of java.io.File objects"
  (vec (.listFiles (File. "files") (proxy [FileFilter] [] (accept [dir] (.isFile dir))))))


; some actions
(def ignore-action (Action. :ignore nil nil))

(def main-menu-action (Action. :update (Menu. :main) main-menu-text))

(def file-menu-action (Action. :update (Menu. :file) file-menu-text))

(defn list-files-action []
  (Action. :update
           (Menu. :file)
           (let [file-names (map #(.getName %) (get-files))]
             (str (clojure.string/join "\n" file-names) "\n"))))

(defn unknown-cmd-action [next-action]
  (let [next-output (:output next-action)]
    (assoc next-action :output (combine-output unknown-cmd-text next-output))))


; some sets of bytes to parse input
(def d-bytes (set (.getBytes "dD" "ascii")))

(def f-bytes (set (.getBytes "fF" "ascii")))

(def m-bytes (set (.getBytes "mM" "ascii")))

(def l-bytes (set (.getBytes "lL" "ascii")))

(def new-line-bytes (set (.getBytes "\r\n" "ascii")))

(extend Menu
  ByteServer
  {:recv (fn [menu in-byte timestamp]
           (case (:state menu)
             :main (cond
                     (new-line-bytes in-byte) ignore-action
                     (f-bytes in-byte) file-menu-action
                     (d-bytes in-byte) (Action. :disconnect (Disconnect. "pressed D at main menu") "goodbye\n")
                     :else (unknown-cmd-action main-menu-action))
             :file (cond
                     (new-line-bytes in-byte) ignore-action
                     (m-bytes in-byte) main-menu-action
                     (l-bytes in-byte) (list-files-action)
                     :else (unknown-cmd-action file-menu-action))))})

(extend Disconnect
  ByteServer
  {:recv (fn [disconnect in-byte timestamp] ignore-action)})
