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
                // We initialize an object output stream from the socket.
                out = new ObjectOutputStream(socket.getOutputStream());
                while (true) {
                    // we then start sending the MyTextEvents from the queue in the DocumentEventCapturer
                    out.writeObject(dec.take());
                    out.flush();
                }
            }
        } catch (Exception e) {
            // If we catch an IOException or InterruptedException we close the stream and terminate.
            try {
                out.close();
            } catch (IOException e1) {
            }
        }
    }
}
