package ddist;

import ddist.events.Event;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.interrupted;

public class EventSender implements Runnable{

    private DocumentEventCapturer dec;
    private Socket socket;
    private ObjectOutputStream outputStream;
    private LinkedBlockingQueue<Event> queue;

    public EventSender(DocumentEventCapturer dec) {
        this.dec = dec;
        socket = null;
        queue = new LinkedBlockingQueue<>();
        receiveLocalEvents();
    }

    private void receiveLocalEvents() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!interrupted()) {
                    try {
                        queue.put((Event) dec.take());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void queueEvent(Event event) {
        try {
            queue.put(event);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        receiveLocalEvents();
        while (!interrupted()) {
            try {
                sendEvent(queue.take());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendEvent(Event event) {
        try {
            outputStream.writeObject(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
