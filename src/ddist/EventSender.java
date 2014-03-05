package ddist;

import ddist.events.AcknowledgeEvent;
import ddist.events.Event;
import ddist.events.text.TextEvent;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class EventSender implements Runnable{

    private DocumentEventCapturer dec;
    private ObjectOutputStream outputStream;
    private LinkedBlockingQueue<Event> queue;
    private boolean receiving = true;
    private EventManager eventManager;
    private CallBack callback;

    public EventSender(DocumentEventCapturer dec, Socket socket, EventManager eventManager, CallBack callback) {
        this.dec = dec;
        this.eventManager = eventManager;
        this.callback = callback;
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
                        if(event instanceof TextEvent){
                            acknowledgeEvent((TextEvent)event);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void acknowledgeEvent(TextEvent event) {
        eventManager.queueEvent(new AcknowledgeEvent(callback.getID(), event.getTimestamp()));

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
                Event event= queue.take();
                sendEvent(event);
                eventManager.queueEvent(event);
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
