(ns serv64.xmodem-test
  (:use clojure.test serv64.xmodem serv64.protocols)
  (:import
    [serv64.xmodem XModem]
    [java.util Arrays]))

(deftest test-xmodem-activate
  "test that on activate an xmodem transfer asks the receiver to begin and goes into a wait state with timeout"
  (let [file-bytes (byte-array (range 500))
        xmodem0 (make-xmodem file-bytes)
        actions (activate xmodem0 0)]
    (is (= [[:send "Activate Xmodem download now.\n"]
            [:replace (XModem. file-bytes :wait 0 60000000000 0)]]
           actions))))

(deftest test-xmodem-cancel
  "test that a <can> message from receiver will cancel a transfer in wait state"
  (let [file-bytes (byte-array (range 500))
        xmodem1 (XModem. file-bytes :wait 0 60000000000 0)
        actions (recv xmodem1 can 1)]
    (is (= [[:pop]]
           actions))))

(deftest test-xmodem-chunk
  "test that an <nak> message from receiver will cause a waiting server to send first chunk"
  (let [file-bytes (byte-array (range 500))
        xmodem1 (XModem. file-bytes :wait 0 60000000000 0)
        actions (recv xmodem1 nak 1)
        [send0 replace] actions
        bytes (get send0 1)]
    (is (= :send (get send0 0)))
    (is (= (+ 3 128 1) (count bytes)))
    ; <soh> (block number: 1) (inverse block number: 254) (bytes 0 to 127) checksum
    (is (= (concat [(byte 1) (byte 1) (unchecked-byte 254)] (byte-array (range 128)) [(checksum (byte-array (range 128)) 0 128)])
           (vec bytes)))
    (is (= [:replace (XModem. file-bytes :next 1 60000000001 0)]
           replace))))

; TODO: more tests