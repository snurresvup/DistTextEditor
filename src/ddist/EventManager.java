package ddist;

import ddist.events.ConnectionEvent;
import ddist.events.Event;
import ddist.events.InitialSetupEvent;
import ddist.events.text.TextEvent;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.interrupted;

public class EventManager implements Runnable {

    private static final double TIME_OFFSET = 0.0001 ;
    private LinkedBlockingQueue<Event> events = new LinkedBlockingQueue<>();

    private EventSender eventSender;
    private Thread est;
    private EventReplayer eventReplayer;
    private JTextArea area;
    private DocumentEventCapturer dec;
    private TimeCallBack time;

    private Socket connection;
    private ObjectInputStream inputStream;
    private double currentClientOffset = 0;


    public EventManager(JTextArea area, DocumentEventCapturer dec, TimeCallBack time) {
        this.area = area;
        this.dec = dec;
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
            ConnectionEvent connectionEvent = (ConnectionEvent) event;
            handleConnectionEvent(connectionEvent);
        }
    }

    private void handleConnectionEvent(ConnectionEvent event) {
        connection = event.getSocket();
        eventReplayer = new EventReplayer(area);
        eventSender = new EventSender(dec);
        est = new Thread(eventSender);
        est.start();
        if (event.isServer()) {
            currentClientOffset += TIME_OFFSET;
            eventSender.queueEvent(new InitialSetupEvent(area.getText(), time.getTime()+currentClientOffset));
        }
    }

    private void handleTextEvent(TextEvent event) {
        if(time.getTime() < event.getTimestamp()) {
            eventReplayer.replayEvent(event);
        } else if(time.getTime() > event.getTimestamp()) {

        }

    }
}
