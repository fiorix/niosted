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

package net.fiorix.niosted;

import net.fiorix.niosted.interfaces.IReactor;
import net.fiorix.niosted.interfaces.IFactory;
import net.fiorix.niosted.interfaces.IProtocol;
import net.fiorix.niosted.interfaces.ITransport;

import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.net.InetSocketAddress;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;

public class Reactor implements IReactor, Runnable
{
    private Selector selector = null;
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private ConcurrentHashMap sockets = new ConcurrentHashMap();
    private ConcurrentHashMap factories = new ConcurrentHashMap();
    private ConcurrentHashMap operations = new ConcurrentHashMap();

    public Reactor() throws IOException
    {
        this.selector = SelectorProvider.provider().openSelector();
    }

    public void TCPServer(InetSocketAddress addr, IFactory factory) throws IOException
    {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(addr);
        channel.register(selector, SelectionKey.OP_ACCEPT);
        this.factories.put(channel, factory);
    }

    public void setInterestOps(SocketChannel channel, int op)
    {
        this.operations.put(channel, op);
    }

    public void connectionLost(SelectionKey key, SocketChannel channel, String reason)
    {
        if(key != null)
            key.cancel();

        try {
            channel.close();
        } catch(IOException ex) {}

        IProtocol protocol = (IProtocol) this.sockets.get(channel);
        if(protocol != null) {
            protocol.connectionLost(reason);
            this.sockets.remove(channel);
        }
    }

    private void onAccept(SelectionKey key) throws IOException
    {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = (SocketChannel) server.accept();
        client.configureBlocking(false);

        IFactory factory = (IFactory) this.factories.get(server);
        IProtocol protocol = factory.makeProtocol();
        protocol.initialize(factory, (new TCPTransport(this, client, protocol)));
        this.sockets.put(client, protocol);

        client.register(this.selector, SelectionKey.OP_READ);
        this.selector.wakeup();
    }

    private void onRead(SelectionKey key)
    {
        SocketChannel channel = (SocketChannel) key.channel();
        IProtocol protocol = (IProtocol) this.sockets.get(channel);
        if(protocol == null) return;

        int bytes = -1;
        byte[] data = null;
        String error = null;

        synchronized(this.readBuffer) {
            try {
                bytes = channel.read(this.readBuffer);
            } catch(IOException ex) {
                this.readBuffer.clear();
                error = ex.getMessage();
            }

            if(bytes == -1) {
                this.readBuffer.clear();
                error = new String("connection closed by remote peer");
            } else {
                data = new byte[bytes];
                System.arraycopy(this.readBuffer.array(), 0, data, 0, bytes);
                this.readBuffer.clear();
            }
        }

        if(error != null) {
            this.connectionLost(key, channel, error);
        } else {
            ITransport transport = protocol.getTransport();
            if(!transport.isClosed()) {
                protocol.dataReceived(data);
            }
        }
    }

    private void onWrite(SelectionKey key)
    {
        SocketChannel channel = (SocketChannel) key.channel();
        IProtocol protocol = (IProtocol) this.sockets.get(channel);
        if(protocol == null) return;

        ITransport transport = protocol.getTransport();
        transport.__write(key);
    }

    public void run()
    {
        while(true) { /* ioloop */

        Set<Map.Entry<SocketChannel, Integer>> ops = this.operations.entrySet();
        if(ops != null) {
            for(Map.Entry<SocketChannel, Integer> item: ops) {
                SocketChannel channel = (SocketChannel) item.getKey();
                if(this.sockets.containsKey(channel)) {
                    SelectionKey key = channel.keyFor(this.selector);
                    if(key != null) {
                        int op = (Integer) item.getValue();
                        key.interestOps(op);
                    }
                }
                this.operations.remove(channel);
            }
            this.selector.wakeup();
        }

        try {
            this.selector.select();
        } catch(IOException ex) {
            ex.printStackTrace();
            break;
        }

        Iterator iter = this.selector.selectedKeys().iterator();
        while(iter.hasNext()) {
            SelectionKey key = (SelectionKey) iter.next();
            iter.remove();

            if(!key.isValid()) continue;
            else if(key.isAcceptable()) {
                try {
                    this.onAccept(key);
                } catch(IOException ex) {
                    ex.printStackTrace();
                }
            } 
            else if(key.isReadable()) this.onRead(key);
            else if(key.isWritable()) this.onWrite(key);
        }

        } /* ioloop */
    }
}
