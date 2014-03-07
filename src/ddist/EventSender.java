package ddist;

import ddist.events.AcknowledgeEvent;
import ddist.events.Event;
import ddist.events.InitialSetupEvent;
import ddist.events.text.TextEvent;
import ddist.events.text.TextInsertEvent;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class EventSender implements Runnable{

    private DocumentEventCapturer dec;
    private ArrayList<ObjectOutputStream> outputStreams = new ArrayList<>();
    private LinkedBlockingQueue<Event> queue = new LinkedBlockingQueue<>();
    private boolean receiving = true;
    private EventManager eventManager;

    public EventSender(DocumentEventCapturer dec, EventManager eventManager, CallBack callback) {
        this.dec = dec;
        this.eventManager = eventManager;
        receiveLocalEvents();
    }

    private void receiveLocalEvents() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (receiving) {
                    try {
                        Event event = dec.take();
                        queueEvent(event);
                        if(event instanceof TextEvent){
                            eventManager.queueEvent(event);
                        }
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
                Event event= queue.take();
                sendEvent(event);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendEvent(Event event) {
        try {
            if(event instanceof AcknowledgeEvent){
                System.out.println("Writing acknowledge on event: " + ((AcknowledgeEvent)event).getEventId() + " \n" +
                        "From: " + ((AcknowledgeEvent)event).getSenderId());
            }else if(event instanceof TextInsertEvent){
                System.out.println("Writing TextEvent " + ((TextInsertEvent)event).getText() + ", " + ((TextInsertEvent) event).getTimestamp());
            }else if(event instanceof InitialSetupEvent) {
                System.out.println("Writing Init event...");
            }
            synchronized (outputStreams) {
                int i = 0;
                for(ObjectOutputStream out : outputStreams){
                    out.writeObject(event);
                    i++;
                    System.out.println(i);
                }
            }
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }

    public void addPeer(Socket socket){
        try {
            synchronized (outputStreams) {
                outputStreams.add(new ObjectOutputStream(socket.getOutputStream()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            receiving = false;
            for(ObjectOutputStream out : outputStreams){
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
