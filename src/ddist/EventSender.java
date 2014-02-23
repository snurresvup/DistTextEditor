package ddist;

import ddist.events.Event;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class EventSender implements Runnable{

    private DocumentEventCapturer dec;
    private ObjectOutputStream outputStream;
    private LinkedBlockingQueue<Event> queue;
    private boolean receiving = true;

    public EventSender(DocumentEventCapturer dec, Socket socket) {
        this.dec = dec;
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        queue = new LinkedBlockingQueue<>();
        receiveLocalEvents();
    }

    private void receiveLocalEvents() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (receiving) {
                    try {
                        Event event = dec.take();
                        queue.put(event);
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
        while (receiving) {
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
            // e.printStackTrace();
        }
    }

    public void close() {
        try {
            receiving = false;
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
