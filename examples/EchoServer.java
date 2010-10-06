import java.io.IOException;
import java.net.InetSocketAddress;

import net.fiorix.niosted.Reactor;
import net.fiorix.niosted.Factory;
import net.fiorix.niosted.interfaces.IProtocol;
import net.fiorix.niosted.protocols.DataReceiver;

public class EchoServer extends Factory
{
    public static void main(String[] args)
    {
        try {
            Reactor reactor = new Reactor();
            reactor.TCPServer(new InetSocketAddress("0.0.0.0", 8888), new EchoServer());

            /* 
             * It could easily become a multi-port echo server,
             * reactor.TCPServer(new InetSocketAddress("0.0.0.0", 9999), new EchoServer());
             */

            new Thread(reactor).start();
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    public IProtocol buildProtocol()
    {
        /*
         * buildProtocol will be called by the Reactor when a new client connection is accepted.
         * An instance of EchoProtocol is created for each individual connection.
         */

        IProtocol protocol = new EchoProtocol();
        return protocol;
    }

    private class EchoProtocol extends DataReceiver
    {
        private String info = null;

        public void connectionMade()
        {
            InetSocketAddress peer = this.transport.getPeer();
            this.info = peer.toString();
            java.lang.System.out.println("client connected: "+this.info);
            this.transport.write("niosted echo server\n");
        }

        public void dataReceived(byte[] data)
        {
            this.transport.write(data);
            // or this.transport.write(new String(data));
        }

        public void connectionLost(String reason)
        {
            java.lang.System.out.println("client disconnected: "+this.info+", "+reason);
        }
    }
}
