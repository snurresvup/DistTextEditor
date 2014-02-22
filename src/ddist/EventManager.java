package ddist;

import ddist.events.ClearTextEvent;
import ddist.events.ConnectionEvent;
import ddist.events.Event;
import ddist.events.InitialSetupEvent;
import ddist.events.text.TextEvent;
import ddist.events.text.TextInsertEvent;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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
    private CallBack callback;

    private Socket connection;
    private ObjectInputStream inputStream;
    private double currentClientOffset = 0;


    public EventManager(JTextArea area, DocumentEventCapturer dec, CallBack time) {
        this.area = area;
        this.dec = dec;
        this.callback = time;
    }

    @Override
    public void run() {
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
                while (!interrupted()){
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
                            queueEvent((Event) input);
                        }
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
            System.out.println("textevent");
            TextEvent textEvent = (TextEvent)event;
            if(textEvent instanceof TextInsertEvent){
                System.out.println(((TextInsertEvent)textEvent).getText());
            }
            handleTextEvent(textEvent);
        } else if(event instanceof ConnectionEvent) {
            System.out.println("connectionevent");
            ConnectionEvent connectionEvent = (ConnectionEvent) event;
            handleConnectionEvent(connectionEvent);
        } else if(event instanceof InitialSetupEvent) {
            System.out.println("initevent");
            InitialSetupEvent initEvent = (InitialSetupEvent)event;
            handleInitialSetupEvent(initEvent);
        } else if(event instanceof ClearTextEvent) {
            System.out.println("clearevent");
            clearTextArea();
        }
    }

    private void clearTextArea() {
        dec.toggleFilter();
        area.setText("");
        dec.toggleFilter();
    }

    private void handleInitialSetupEvent(InitialSetupEvent initEvent) {
        queueEvent(new ClearTextEvent(callback.getTime()));
        callback.setID(initEvent.getTime4Client()-initEvent.getTimestamp());
        callback.setTime(initEvent.getTime4Client());
        queueEvent(new TextInsertEvent(0, initEvent.getAreaText(), callback.getTime()));
    }

    private void handleConnectionEvent(ConnectionEvent event) {
        connection = event.getSocket();
        try {
            eventSender = new EventSender(dec, connection);
            inputStream = new ObjectInputStream(connection.getInputStream());
            est = new Thread(eventSender);
            est.start();
            startEventReceiverThread();
        } catch (IOException e) {
            e.printStackTrace();
        }
        eventReplayer = new EventReplayer(area, dec);
        callback.setTitleOfWindow("Connected!!!");
        callback.setConnect(false);
        callback.setDisconnect(true);
        callback.setListen(false);
        callback.setStopListening(false);
        if (event.isServer()) {
            callback.setID(0);
            currentClientOffset += TIME_OFFSET;
            eventSender.queueEvent(new InitialSetupEvent(area.getText(), callback.getTime() + currentClientOffset, callback.getTime()));
        }
    }

    private void handleTextEvent(TextEvent event) {
        eventReplayer.replayEvent(event);
        if(callback.getTime() < event.getTimestamp()) {
            eventReplayer.replayEvent(event);
        } else if(callback.getTime() > event.getTimestamp()) {

        }
    }
}
