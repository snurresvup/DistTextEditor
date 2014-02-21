package ddist.events;

import java.net.Socket;

public class ConnectionEvent implements Event{
    private Socket socket;
    private double timestamp;

    public ConnectionEvent(Socket socket, double timestamp) {
        this.socket = socket;
        this.timestamp = timestamp;
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
}
