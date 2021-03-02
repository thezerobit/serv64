(ns serv64.actions-test
  (:require [clojure.test :refer :all]
            [serv64.actions :refer :all])
  (:import (serv64.actions Menu)))

(defn char-to-byte [char]
  (get (.getBytes (String/valueOf char) "ascii") 0))

(deftest test-menu-navigation
  ; pressing F in main menu takes you to the file menu
  (let [result-action (recv (Menu. :main) (char-to-byte \f) 0)]
    (is (= (:state result-action) (Menu. :file))))

  ; pressing M in file menu takes you to main menu
  (let [result-action (recv (Menu. :file) (char-to-byte \m) 0)]
    (is (= (:state result-action) (Menu. :main))))
  )
