package ddist.events;

import java.net.Socket;

public class JoinEvent implements Event{
    private Socket socket;

    public JoinEvent(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }
}
