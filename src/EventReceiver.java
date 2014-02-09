import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class EventReceiver implements Runnable {
    private Socket socket;
    private LinkedBlockingQueue<MyTextEvent> incomingEvents;
    private EventReplayer er;

    public EventReceiver(Socket socket, LinkedBlockingQueue<MyTextEvent> incomingEvents, EventReplayer er) {
        this.socket = socket;
        this.incomingEvents = incomingEvents;
        this.er = er;
    }

    @Override
    public void run() {
        try {
            if (socket != null) {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                while (true) {
                    MyTextEvent mte = (MyTextEvent) in.readObject();
                    incomingEvents.add(mte);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            er.handleClosedSocket();
        }
    }
}
