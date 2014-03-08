package ddist.events;

import java.net.Socket;

public class ConnectionEvent implements Event{
    private Socket socket;
    private int listeningPort;

    public ConnectionEvent(Socket socket, int listeningPort) {
        this.socket = socket;
        this.listeningPort = listeningPort;
    }

    public Socket getSocket() {
        return socket;
    }

    public int getListeningPort() {
        return listeningPort;
    }
}
