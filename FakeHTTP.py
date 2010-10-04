#!/usr/bin/env python
# coding: utf-8
#
# Copyright 2010 Alexandre Fiori
# niosted
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

import sys
sys.path.append("niosted.jar")

from java.net import InetSocketAddress
from net.fiorix.niosted import Factory
from net.fiorix.niosted import TCPServer
from net.fiorix.niosted.protocols import LineReceiver

class FakeProtocol(LineReceiver):
    def lineReceived(self, line):
        #if line:
        #    print "got header:", line
        #else:

        if not line:
            self.transport.write("HTTP/1.1 200 OK\r\n"+
            "Etag: \"b32ffe9242ccde98d6e19ed4c9dbc053d4a18155\"\r\n"+
            "Content-Length: 14\r\n"+
            "Content-Type: text/html; charset=UTF-8\r\n"+
            "Server: CycloneServer/0.4\r\n\r\n"+
            "Hello, world\r\n")
            self.transport.loseConnection()

    def connectionMade(self):
        #print "new client connected:", self.transport.getPeer()
        pass

    def connectionLost(self, reason):
        #print "connection lost:", reason
        pass

class FakeFactory(Factory):
    def makeProtocol(self):
        return FakeProtocol()

if __name__ == "__main__":
    addr = InetSocketAddress("0.0.0.0", 8888)
    TCPServer(addr, FakeFactory()).run()
