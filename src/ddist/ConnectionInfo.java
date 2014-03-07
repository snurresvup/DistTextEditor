package ddist;


import java.io.Serializable;
import java.net.InetAddress;

public class ConnectionInfo implements Serializable{
    private InetAddress inetAddress;
    private int port;

    public ConnectionInfo(InetAddress inetAddress, int port) {
        this.inetAddress = inetAddress;
        this.port = port;
    }

    public InetAddress getInetAddress(){
        return inetAddress;
    }

    public int getPort(){
        return port;
    }
}
