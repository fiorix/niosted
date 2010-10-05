package net.fiorix.niosted.interfaces;

import java.net.InetSocketAddress;

public interface IFactory
{
    public IProtocol makeProtocol();
}
