(ns serv64.actions-test
  (:require [clojure.test :refer :all]
            [serv64.menu :refer :all])
  (:import (serv64.menu MenuServer)))

(defn char-to-byte [char]
  (get (.getBytes (String/valueOf char) "ascii") 0))

(deftest test-menu-navigation
  ; pressing F in main menu takes you to the file menu

  ; pressing M in file menu takes you to main menu
  )
