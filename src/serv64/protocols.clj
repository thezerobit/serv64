(ns serv64.protocols)

; the protocol that represents a functional interface for
; a stateful server
(defprotocol ByteServer
  (activate [state timestamp] "returns an ordered collection of actions")
  (recv [state msgBytes timestamp] "returns an ordered collection of actions"))

; known actions:
; [:disconnect] server should disconnect immediately
; [:send output] server should send some data back to the client, output should be string or byte array
; [:push state] server should push a new state onto the stack
; [:pop] server should pop a state off the stack
; [:replace state] replace the top of the server stack
; [:list-files] server should send a list of available files
; [:xmodem index] server should initiate an xmodem upload of file
