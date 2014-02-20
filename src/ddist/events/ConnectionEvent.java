package ddist.events;

import java.net.Socket;

public class ConnectionEvent {
    private Socket socket;

    public ConnectionEvent(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }
}
