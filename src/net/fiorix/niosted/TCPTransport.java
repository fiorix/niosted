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
import net.fiorix.niosted.interfaces.IProtocol;
import net.fiorix.niosted.interfaces.ITransport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;


public class TCPTransport implements ITransport
{
    private boolean closed = false;
    private boolean flushing = false;
    private boolean autoFlush = true;
    private IProtocol protocol = null;
    private IReactor reactor = null;
    private SocketChannel channel = null;
    private ByteBuffer readBuffer = ByteBuffer.allocate(4096);
    private ConcurrentLinkedQueue writeBuffer = new ConcurrentLinkedQueue();

    public TCPTransport(IReactor reactor, SocketChannel channel, IProtocol protocol)
    {
        this.channel = channel;
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
        this.reactor.setInterestOps(this.channel, SelectionKey.OP_WRITE);
    }

    public void flush()
    {
        if(this.closed || this.flushing) return; // should throw exception
        if(!this.writeBuffer.isEmpty()) {
            this.flushing = true;
            this.reactor.setInterestOps(this.channel, SelectionKey.OP_WRITE);
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
        if(!this.flushing)
            this.reactor.connectionLost(null, this.channel, "connection closed cleanly");
    }

    public InetSocketAddress getPeer()
    {
        return (InetSocketAddress) this.channel.socket().getRemoteSocketAddress();
    }

    public boolean isClosed()
    {
        return this.closed;
    }

    public void __read(SelectionKey key)
    {
        int bytes = -1;

        try {
            bytes = this.channel.read(this.readBuffer);
        } catch(IOException ex) {
            this.readBuffer.clear();
            this.reactor.connectionLost(key, this.channel, ex.getMessage());
            return;
        }

        if(bytes == -1) {
            this.readBuffer.clear();
            this.reactor.connectionLost(key, this.channel, "connection closed by remote peer");
            return;
        }

        byte[] data = new byte[bytes];
        System.arraycopy(this.readBuffer.array(), 0, data, 0, bytes);
        this.readBuffer.clear();

        if(!this.closed)
            this.protocol.dataReceived(data);
    }

    public void __write(SelectionKey key)
    {
        while(!this.writeBuffer.isEmpty()) {
            ByteBuffer buffer = (ByteBuffer) this.writeBuffer.peek();
            try {
                this.channel.write(buffer);
            } catch(IOException ex) {
                this.reactor.connectionLost(key, this.channel, ex.getMessage());
                return;
            }
            if(buffer.remaining() >= 1) break;
            this.writeBuffer.remove(buffer);
        }

        this.flushing = false;
        if(this.closed)
            this.loseConnection();
        else
            this.reactor.setInterestOps(this.channel, SelectionKey.OP_READ);
    }
}
