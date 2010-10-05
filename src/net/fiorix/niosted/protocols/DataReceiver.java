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

package net.fiorix.niosted.protocols;

import net.fiorix.niosted.interfaces.IFactory;
import net.fiorix.niosted.interfaces.IProtocol;
import net.fiorix.niosted.interfaces.ITransport;

public class DataReceiver implements IProtocol
{
    public IFactory factory;
    public ITransport transport;

    public void dataReceived(byte[] data)
    {
    }

    public void connectionMade()
    {
    }

    public void connectionLost(String reason)
    {
    }

    public void initialize(IFactory factory, ITransport transport)
    {
        this.factory = factory;
        this.transport = transport;
        this.connectionMade();
    }

    public ITransport getTransport()
    {
        return this.transport;
    }
}
