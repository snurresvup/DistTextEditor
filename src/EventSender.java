import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class EventSender implements Runnable {
    private Socket socket;
    private DocumentEventCapturer dec;
    private EventReplayer er;
    private ObjectOutputStream out;

    public EventSender(Socket socket, DocumentEventCapturer dec, EventReplayer er) {
        this.socket = socket;
        this.dec = dec;
        this.er = er;
    }

    @Override
    public void run() {
        try {
            if (socket != null) {
                out = new ObjectOutputStream(socket.getOutputStream());
                while (true) {
                    out.writeObject(dec.take());
                    out.flush();
                }
            }
        } catch (Exception e) {
            try {
                out.close();
            } catch (IOException e1) {
                // e1.printStackTrace();
            }
        }
    }
}
