(ns serv64.utils)

; Converting the output to bytes (array of byte).
(defmulti to-byte-array class)

(defmethod to-byte-array String [str] (.getBytes str "ascii"))

; this is the byte[] implementation
(defmethod to-byte-array (Class/forName "[B") [arr] arr)