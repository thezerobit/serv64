(ns serv64.xmodem
  (:use serv64.protocols)
  (:import
    [java.util.concurrent TimeUnit]
    [java.util Arrays]))

; implementation of classic XMODEM protocol (sender)
; as described here: https://techheap.packetizer.com/communication/modems/xmodem.html
; with the following provisions:
;
; 1. XMODEM only sends 128 byte chunks, there is no last/short chunk and the above document
; does not address the issue, which leaves the question of padding. This implementation
; pads with zero bytes.
;
; 2. This implementation does not address issues with sending files to a CP/M machine
; ending with a \r\n on a 128 byte boundary as mentioned in the anachronistic specification.
;
; 3. This implementation allows <can> from receiver to cancel transmission, an optional feature.

(def nak (byte-array [(byte 21)]))

(def ack (byte-array [(byte 6)]))

(def can (byte-array [(byte 24)]))

(def eot (byte-array [(byte 4)]))

(defn to-block-number [idx]
  "block numbers start at 1 and go up to 255 before wrapping back to 1"
  (+ 1 (mod idx 255)))

(defn byte-to-int [b]
  "convert a byte to its unsigned value as an int, the inverse is unchecked-byte"
  (bit-and b 255))

(defn checksum [array start end]
  "adds up all the unsigned bytes in the range truncating result to bottom 8 bits"
  (let [values (map (fn [idx] (byte-to-int (get array idx))) (range start end))]
    (unchecked-byte (reduce + values))))

(defn remaining-bytes [file-bytes chunk-idx]
  (-> file-bytes (count) (- (* chunk-idx 128))))

(defn make-chunk [file-bytes chunk-idx bytes-left]
  "block number, 255 - block number, <128 bytes>, checksum"
  (let [block-number (to-block-number chunk-idx)
        chunk (byte-array (+ 3 128 1))]                     ; 3 byte header, 128 bytes of data, 1 byte checksum
    (aset-byte chunk 0 (unchecked-byte 1))                  ; SOH (0x01)
    (aset-byte chunk 1 (unchecked-byte block-number))
    (aset-byte chunk 2 (unchecked-byte (- 255 block-number)))
    (System/arraycopy file-bytes (* chunk-idx 128) chunk 3 (Math/min 128 bytes-left))
    (aset-byte chunk 131 (checksum chunk 3 131))            ; checksum byte
    chunk))

(defn seconds-to-nanos [seconds] (.toNanos TimeUnit/SECONDS seconds))

; the client is attempting to download a file
(defrecord XModem [file-bytes state next-chunk-idx timeout retries])

(extend XModem
  ByteServer
  {:activate (fn [xmodem timestamp]
               (case (:state xmodem)
                 :negotiate [[:send "Activate Xmodem download now.\n"]
                             [:replace (assoc xmodem :state :wait :timeout (+ timestamp (seconds-to-nanos 60)))]]
                 []))
   :recv     (fn [xmodem msgBytes timestamp]
               (let [chunk-idx (:next-chunk-idx xmodem)
                     time-out? (> timestamp (:timeout xmodem))
                     file-bytes (:file-bytes xmodem)
                     bytes-left (remaining-bytes file-bytes chunk-idx)
                     retries (:retries xmodem)]
                 (if (or time-out? (Arrays/equals can msgBytes))
                   ; cancel transfer on timeout or <can>
                   [[:pop]]
                   ; otherwise process based on state
                   (case (:state xmodem)
                     :wait (cond
                             ; <nak> on first chunk, send it
                             (Arrays/equals nak msgBytes)
                             [[:send (make-chunk file-bytes chunk-idx bytes-left)]
                              [:replace (assoc xmodem :state :next :next-chunk-idx (inc chunk-idx)
                                                      :timeout (+ timestamp (seconds-to-nanos 60))
                                                      :retries 0)]]
                             ; commands other than <can> or <nak> are ignored in this initial wait state
                             :else [])
                     :next (cond
                             ; <nak> -> resend previous chunk
                             (Arrays/equals nak msgBytes)
                             (if (< retries 10)
                               (let [bytes-left (remaining-bytes file-bytes (dec chunk-idx))]
                                 [[:send (make-chunk file-bytes (dec chunk-idx) bytes-left)]
                                  [:replace (assoc xmodem
                                              :timeout (+ timestamp (seconds-to-nanos 60))
                                              :retries (inc retries))]])
                               [[:pop]])
                             ; <ack> -> send next chunk
                             (Arrays/equals ack msgBytes)
                             (if (> bytes-left 0)
                               [[:send (make-chunk file-bytes chunk-idx bytes-left)]
                                [:replace (assoc xmodem :state :next
                                                        :timeout (+ timestamp (seconds-to-nanos 60))
                                                        :retries 0)]]
                               [[:send eot]
                                [:replace (assoc xmodem :state :final :retries 0)]]))
                     :final (cond
                              ; <nak> -> resend <eot>
                              (Arrays/equals nak msgBytes)
                              (if (< retries 10)
                                [[:send eot]
                                 [:replace (assoc xmodem :retries (inc retries))]]
                                [[:pop]])
                              ; <ack> -> we're done!
                              (Arrays/equals ack msgBytes)
                              [[:pop]])))))})

(defn make-xmodem [file-bytes] (XModem. file-bytes :negotiate 0 0 0))