# serv64

A simple BBS-like telnet server.

## Implementation Notes

How to write a functional stateful server?

When a byte is received, call an update function that takes
the current state and the byte received and a timestamp and
returns some data that instructs the program
whether to do nothing (:ignore), update the state (:update),
or disconnect (:disconnect) and also whether to send any
data back to the client or not. This update function is purely
functional and therefore easy to fully test with unit tests.

## Usage

To start a server on port 8080:

    lein run
    
Telnet to this port to test it out:

    telnet localhost 8080

## License

Copyright © 2021 Stephen A. Goss

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
