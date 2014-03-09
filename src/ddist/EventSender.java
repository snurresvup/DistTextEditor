package ddist;

import ddist.events.AcknowledgeEvent;
import ddist.events.Event;
import ddist.events.InitialSetupEvent;
import ddist.events.text.TextEvent;
import ddist.events.text.TextInsertEvent;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class EventSender implements Runnable{

    private DocumentEventCapturer dec;
    private HashMap<Double, ObjectOutputStream> outputStreams = new HashMap<>();
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
                        // Take an event from the queue of local events
                        Event event = dec.take();
                        // Queue the event in the sender queue. This queue contains the events that should be sent to other peers.
                        queueEvent(event);
                        // Queue the same event in our own event manager
                        eventManager.queueEvent(event); //TODO removed instanceof check here does it still work?
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
                // Take an event from the outgoing queue.
                Event event= queue.take();
                // Pass it to the sendEvent helper method.
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
            // We synchronize to avoid concurrent modification.
            synchronized (outputStreams) {
                // For each output stream, write the object to that stream.
                for(ObjectOutputStream out : outputStreams.values()){
                    out.writeObject(event);
                }
            }
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }

    public void addPeer(double client, Socket socket){
        try {
            System.out.println("AddPeer: [id: " + client + ", Socket: "+ socket.getRemoteSocketAddress() +"]");
            synchronized (outputStreams) {
                outputStreams.put(client, new ObjectOutputStream(socket.getOutputStream()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            receiving = false;
            for(ObjectOutputStream out : outputStreams.values()){
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendEventToPeer(Event event, double client) {
        try {
            outputStreams.get(client).writeObject(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removePeer(double peerId) {
        synchronized (outputStreams) {
            try {
                outputStreams.get(peerId).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStreams.remove(peerId);
        }
    }
}
