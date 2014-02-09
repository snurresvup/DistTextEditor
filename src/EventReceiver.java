import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class EventReceiver implements Runnable {
    private Socket socket;
    private LinkedBlockingQueue<MyTextEvent> receivedEvents;
    private EventReplayer er;

    public EventReceiver(Socket socket, LinkedBlockingQueue<MyTextEvent> receivedEvents, EventReplayer er) {
        this.socket = socket;
        this.receivedEvents = receivedEvents;
        this.er = er;
    }

    @Override
    public void run() {
        try {
            if (socket != null) {
                // We initialise an object input stream from the socket
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                while (true) {
                    // Then we start listening for incoming MyTextEvents. Upon receiving one we add it to the queue.
                    MyTextEvent mte = (MyTextEvent) in.readObject();
                    receivedEvents.add(mte);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            // If the socket is closed from one of the sides we catch this IOException on both sides, and then we
            // handle things so that the client goes back to local mode, and the server starts listening again.
            er.handleClosedSocket();
        }
    }
}
