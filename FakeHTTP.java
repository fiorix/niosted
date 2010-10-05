/*
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
*/

import java.io.IOException;
import java.net.InetSocketAddress;

import net.fiorix.niosted.TCPServer;
import net.fiorix.niosted.Factory;
import net.fiorix.niosted.Protocol;
import net.fiorix.niosted.protocols.LineReceiver;

public class FakeHTTP implements Factory
{
    public static void main(final String[] args)
    {
        Factory factory = new FakeHTTP();

        try {
            InetSocketAddress addr = new InetSocketAddress("0.0.0.0", 8888);
            new Thread(new TCPServer(addr, factory)).start();
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    public Protocol makeProtocol() {
        return new FakeProtocol();
    }

    private class FakeProtocol extends LineReceiver
    {
        public void lineReceived(String line)
        {
            /*
            if(line != null)
                java.lang.System.out.println("got header: "+line);
            else {
            */

            if(line == null) {
                this.transport.write(
                    "HTTP/1.1 200 OK\r\n"+
                    "Etag: \"b32ffe9242ccde98d6e19ed4c9dbc053d4a18155\"\r\n"+
                    "Content-Length: 14\r\n"+
                    "Content-Type: text/html; charset=UTF-8\r\n"+
                    "Server: CycloneServer/0.4\r\n\r\n"+
                    "Hello, world\r\n");
                this.transport.loseConnection();
            }
        }

        public void connectionMade()
        {
            /*
            InetSocketAddress peer = this.transport.getPeer();
            String ip = (String) peer.getAddress().getHostAddress();
            int port = peer.getPort();
            java.lang.System.out.println("new client connected: "+ip+":"+port);
            */
        }

        public void connectionLost(String reason)
        {
            //java.lang.System.out.println("connection lost: "+reason);
        }
    }
}
