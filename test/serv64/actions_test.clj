(ns serv64.actions-test
  (:require [clojure.test :refer :all]
            [serv64.servers :refer :all])
  (:import (serv64.servers MenuServer)))

(defn char-to-byte [char]
  (get (.getBytes (String/valueOf char) "ascii") 0))

(deftest test-menu-navigation
  ; pressing F in main menu takes you to the file menu
  (let [result-action (recv (MenuServer. :main) (char-to-byte \f) 0)]
    (is (= (:state result-action) (MenuServer. :file))))

  ; pressing M in file menu takes you to main menu
  (let [result-action (recv (MenuServer. :file) (char-to-byte \m) 0)]
    (is (= (:state result-action) (MenuServer. :main))))
  )
