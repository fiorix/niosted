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

import java.io.IOException;

import java.util.Set;
import java.util.Map;
import java.util.Iterator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.net.InetSocketAddress;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;


public class TCPServer implements Reactor, Runnable
{
    private Factory factory = null;
    private Selector selector = null;
    private InetSocketAddress sockaddr = null;
    private ServerSocketChannel channel = null;
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private ConcurrentHashMap operations = new ConcurrentHashMap();
    private ConcurrentHashMap connections = new ConcurrentHashMap();

    public TCPServer(InetSocketAddress sockaddr, Factory factory) throws IOException
    {
        this.factory = factory;
        this.sockaddr = sockaddr;
        this.selector = SelectorProvider.provider().openSelector();
        this.channel = ServerSocketChannel.open();
        this.channel.configureBlocking(false);
        this.channel.socket().bind(this.sockaddr);
        this.channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void setInterestOps(SocketChannel channel, int op)
    {
        this.operations.put(channel, op);
    }

    public void connectionLost(SelectionKey key, SocketChannel client, String reason)
    {
        key.cancel();
        try { client.close(); } catch(IOException ign) {}
        Protocol proto = (Protocol) this.connections.get(client);
        if(proto != null) {
            proto.connectionLost(reason);
            this.connections.remove(client);
        }
    }

    private void IOLoop() throws TCPServerException
    {
        int pending = this.operations.size();
        Set<Map.Entry<SocketChannel, Integer>> ops = this.operations.entrySet();
        if(ops!=null) {
            for(Map.Entry<SocketChannel, Integer> item: ops) {
                SocketChannel client = (SocketChannel) item.getKey();
                if(this.connections.containsKey(client)) {
                    SelectionKey key = client.keyFor(this.selector);
                    if(key != null) {
                        int op = (Integer) item.getValue();
                        key.interestOps(op);
                    }
                }
                this.operations.remove(client);
            }
            if(pending >= 1)
                this.selector.wakeup();
        }

        try {
            this.selector.select();
        } catch(IOException ex) {
            ex.printStackTrace();
            throw new TCPServerException();
        }

        Iterator iter = this.selector.selectedKeys().iterator();
        while(iter.hasNext()) {
            SelectionKey key = (SelectionKey) iter.next();
            iter.remove();

            if(!key.isValid()) continue;
            else if(key.isAcceptable()) {
                try { 
                    this.accept(key);
                } catch(IOException ex) {
                    ex.printStackTrace();
                    throw new TCPServerException();
                }
            }
            else if(key.isReadable()) this.read(key);
            else if(key.isWritable()) this.write(key);
        }
    }

    private void accept(SelectionKey key) throws IOException
    {
        SocketChannel client = this.channel.accept();
        client.configureBlocking(false);
        client.register(this.selector, SelectionKey.OP_READ);

        Protocol proto = this.factory.makeProtocol();
        proto.initialize(this.factory, (new TCPTransport(this, proto, client)));
        this.connections.put(client, proto);
        this.selector.wakeup();
    }

    private void read(SelectionKey key)
    {
        SocketChannel client = (SocketChannel) key.channel();
        Protocol proto = (Protocol) this.connections.get(client);
        if(proto == null) return;

        int bytes;
        try {
            bytes = client.read(this.readBuffer);
        } catch(IOException ex) {
            this.connectionLost(key, client, ex.getMessage());
            return;
        }

        if(bytes == -1) {
            this.connectionLost(key, client, "client disconnected");
            return;
        }

        Transport transport = proto.getTransport();
        if(!transport.isClosed()) {
            byte[] data = new byte[bytes];
            System.arraycopy(this.readBuffer.array(), 0, data, 0, bytes);
            this.readBuffer.clear();
            proto.dataReceived(data);
        }
    }

    private void write(SelectionKey key)
    {
        SocketChannel client = (SocketChannel) key.channel();
        Protocol proto = (Protocol) this.connections.get(client);
        if(proto == null) return;

        Transport transport = proto.getTransport();
        transport.__doWrite(key);
    }

    public void run()
    {
        try {
            while(true) IOLoop();
        } catch(TCPServerException ex) {}
    }

    private class TCPTransport implements Transport
    {
        private boolean closed = false;
        private boolean flushing = false;
        private boolean autoFlush = true;
        private Protocol protocol = null;
        private Reactor reactor = null;
        private SocketChannel client = null;
        private ConcurrentLinkedQueue writeBuffer = new ConcurrentLinkedQueue();

        public TCPTransport(Reactor reactor, Protocol protocol, SocketChannel client)
        {
            this.client = client;
            this.reactor = reactor;
            this.protocol = protocol;
        }

        public void write(String data)
        {
            if(this.closed) return; // should throw exception
            this.write(data.getBytes());
        }

        public void write(byte[] data)
        {
            if(this.closed) return; // should throw exception
            this.writeBuffer.add(ByteBuffer.wrap(data));
            this.reactor.setInterestOps(this.client, SelectionKey.OP_WRITE);
        }

        public void flush()
        {
            if(this.closed || this.flushing) return; // should throw exception
            if(!this.writeBuffer.isEmpty()) {
                this.flushing = true;
                this.reactor.setInterestOps(this.client, SelectionKey.OP_WRITE);
            }
        }

        public void setAutoFlush(boolean v)
        {
            this.autoFlush = v;
        }

        public void loseConnection()
        {
            if(this.autoFlush)
                this.flush();

            this.closed = true;
            if(!this.flushing) {
                try { 
                    this.client.close();
                    this.protocol.connectionLost("connection closed by remote peer");
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        public InetSocketAddress getPeer()
        {
            return (InetSocketAddress) this.client.socket().getRemoteSocketAddress();
        }

        public boolean isClosed()
        {
            return this.closed;
        }

        public void __registerProtocol(Protocol proto)
        {
            this.protocol = proto;
        }

        public void __doWrite(SelectionKey key)
        {
            while(!this.writeBuffer.isEmpty()) {
                ByteBuffer buffer = (ByteBuffer) this.writeBuffer.peek();
                try {
                    this.client.write(buffer);
                } catch(IOException ex) {
                    this.reactor.connectionLost(key, this.client, ex.getMessage());
                    return;
                }
                if(buffer.remaining() >= 1) break;
                this.writeBuffer.remove(buffer);
            }

            this.flushing = false;
            if(this.closed)
                this.loseConnection();
            else
                key.interestOps(SelectionKey.OP_READ);
        }
    }

    private class TCPServerException extends Exception
    {
    }
}
