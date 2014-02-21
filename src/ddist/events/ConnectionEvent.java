package ddist.events;

import java.net.Socket;

public class ConnectionEvent implements Event{
    private Socket socket;
    private double timestamp;
    private boolean server;

    public ConnectionEvent(Socket socket, double timestamp, boolean server) {
        this.socket = socket;
        this.timestamp = timestamp;
        this.server = server;
    }

    public Socket getSocket() {
        return socket;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isServer() {
        return server;
    }
}
