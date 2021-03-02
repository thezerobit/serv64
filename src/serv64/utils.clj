(ns serv64.utils)

; Converting the output to bytes (array of byte).
(defmulti to-byte-array class)

(defmethod to-byte-array String [str] (.getBytes str "ascii"))

; this is the byte[] implementation
(defmethod to-byte-array (Class/forName "[B") [arr] arr)

; Combine the output of two actions, optimized for combining 2 string inputs
; or just converting outputs to bytes and returning a combined array of bytes.
(defn combine-output [fst snd]
  (cond
    (and (isa? (class fst) String) (isa? (class snd) String)) (str fst snd)
    :else (into-array (concat (to-byte-array fst) (to-byte-array snd)))))


