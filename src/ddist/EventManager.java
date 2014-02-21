package ddist;

import ddist.events.ConnectionEvent;
import ddist.events.Event;
import ddist.events.text.TextEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.interrupted;

public class EventManager implements Runnable {

    private LinkedBlockingQueue<Event> events = new LinkedBlockingQueue<>();

    private final EventSender eventSender;
    private EventReplayer eventReplayer;
    private TimeCallBack time;

    private Socket connection;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;


    public EventManager(EventReplayer eventReplayer, EventSender eventSender, TimeCallBack time) {
        this.eventReplayer = eventReplayer;
        this.eventSender = eventSender;
        this.time = time;
    }

    @Override
    public void run() {
        startEventReceiverThread();
        while (!interrupted()){
            try {
                Event event = events.take();
                handleEvent(event);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void startEventReceiverThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(connection != null && connection.isConnected()){
                    Object input = null;
                    try {
                        input = inputStream.readObject();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    if(input instanceof Event){
                        queueEvent((Event)input);
                    }
                }
            }
        }).start();

    }

    public void queueEvent(Event event){
        try {
            events.put(event);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleEvent(Event event) {
        if(event instanceof TextEvent) {
            TextEvent textEvent = (TextEvent)event;
            handleTextEvent(textEvent);
        } else if(event instanceof ConnectionEvent) {
            ConnectionEvent connectionEvent = (ConnectionEvent)event;
            handleConnectionEvent(connectionEvent);
        }
    }

    private void handleConnectionEvent(ConnectionEvent event) {
        connection = event.getSocket();

    }

    private Event receiveEvent() {
        if(connection != null){
            Object input = null;
            try {
                input = inputStream.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if(input instanceof Event) {
                final Event event = (Event)input;
                return event;
            }
        }

        return null;
    }

    private void handleTextEvent(TextEvent event) {
        if(time.getTime() < event.getTimestamp()) {
            eventReplayer.replayEvent(event);
        } else if(time.getTime() > event.getTimestamp()) {

        }

    }

    public void newConnection(Socket socket){
        connection = socket;
        try {
            inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            try {
                if(connection != null){
                    connection.close();
                }
            } catch (IOException e1) {}
        }
    }
}
