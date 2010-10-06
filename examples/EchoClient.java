import java.io.IOException;
import java.net.InetSocketAddress;

import net.fiorix.niosted.Reactor;
import net.fiorix.niosted.ClientFactory;
import net.fiorix.niosted.interfaces.IReactor;
import net.fiorix.niosted.interfaces.IProtocol;
import net.fiorix.niosted.protocols.LineReceiver;

public class EchoClient extends ClientFactory
{
    public IReactor reactor = null;

    public static void main(String[] args)
    {
        int port = 0;
        String host = null;

        try {
            host = args[0];
            port = Integer.parseInt(args[1]);
        } catch(Exception ex) {
            java.lang.System.out.println("use: EchoClient host port");
            System.exit(1);
        }

        try {
            Reactor reactor = new Reactor();
            reactor.TCPClient(new InetSocketAddress(host, port), new EchoClient(reactor));

            /* 
             * It could easily connect to multiple servers,
             * reactor.TCPClient(new InetSocketAddress("4.2.2.2", 9999), new EchoClient(reactor));
             */

            new Thread(reactor).start();
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    public EchoClient(IReactor reactor)
    {
        this.reactor = reactor;
    }

    public IProtocol buildProtocol()
    {
        IProtocol protocol = new EchoProtocol();
        return protocol;
    }

    public void clientConnectionFailed(IReactor reactor, InetSocketAddress addr, String reason)
    {
        java.lang.System.out.println("connection failed: "+addr.toString()+", "+reason);
        this.reactor.stop();
    }

    private class EchoProtocol extends LineReceiver
    {
        public void connectionMade()
        {
            this.delimiter = "\n";
            java.lang.System.out.println("connected to server");

            this.transport.write("Hello, world\nThis is the Echo Client\n");
            this.sendLine("Cool, isn't it?");
            this.sendLine("quit");
        }

        public void lineReceived(String line)
        {
            if(line.contentEquals("quit")) {
                EchoClient factory = (EchoClient) this.factory;
                factory.reactor.stop();
            } else {
                java.lang.System.out.println("line received: ("+line+")");
            }
        }

        public void connectionLost(String reason)
        {
            java.lang.System.out.println("connection lost: "+reason);
        }
    }
}
