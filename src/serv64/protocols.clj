(ns serv64.protocols)

; the protocol that represents a functional interface for
; a stateful server
(defprotocol ByteServer
  (recv [state in-byte timestamp] "returns an ordered collection of actions"))

; known actions:
; [:disconnect] server should disconnect immediately
; [:send output] server should send some data back to the client, output should be string or byte array
; [:push state] server should push a new state onto the stack
; [:pop] server should pop a state off the stack
; [:list-files] server should send a list of available files
