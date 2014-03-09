package ddist.events;

import java.net.Socket;

public class JoinEvent implements Event{
    private Socket socket;
    private static final long serialVersionUID = 1L;

    public JoinEvent(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }
}
