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

package net.fiorix.niosted.interfaces;

import net.fiorix.niosted.Factory;
import net.fiorix.niosted.ClientFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public interface IReactor
{
    public void stop();

    public void TCPServer(InetSocketAddress addr, Factory factory) throws IOException;
    public void TCPClient(InetSocketAddress addr, ClientFactory factory) throws IOException;

    public void setInterestOps(SocketChannel channel, int op);
    public void connectionLost(SelectionKey key, SocketChannel client, String reason);
}
