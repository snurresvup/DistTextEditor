package ddist;

import ddist.events.*;
import ddist.events.text.*;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;
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

    private SortedMap<Double, TextEvent> log = Collections.synchronizedSortedMap(new TreeMap<Double, TextEvent>());

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
            boolean receiving = true;
            @Override
            public void run() {
                while (receiving){
                    if(connection != null && connection.isConnected()){
                        Object input = null;
                        try {
                            input = inputStream.readObject();
                        } catch (IOException e) {
                            queueEvent(new DisconnectEvent());
                            receiving = false;
                            break;
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
        } else if(event instanceof DisconnectEvent) {
            handleDisconnectEvent();
        }
    }

    private void handleDisconnectEvent() {
        try {
            eventSender.close();
            inputStream.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        callback.setTitleOfWindow("Disconnected");
        callback.setDisconnect(false);
        if (callback.isServer()) {
            callback.setStopListening(true);
        } else {
            callback.setConnect(true);
            callback.setListen(true);
        }
    }

    private void clearTextArea() {
        dec.setFilter(false);
        area.setText("");
        dec.setFilter(true);
    }

    private void handleInitialSetupEvent(InitialSetupEvent initEvent) {
        queueEvent(new ClearTextEvent(callback.getTime()));
        callback.setID(initEvent.getTime4Client()-initEvent.getTimestamp());
        callback.setTime(initEvent.getTime4Client());
        queueEvent(new TextInsertEvent(0, initEvent.getAreaText(), callback.getTime()));
    }

    private void handleConnectionEvent(ConnectionEvent event) {
        connection = event.getSocket();
        events.clear();
        try {
            eventSender = new EventSender(dec, log, connection);
            inputStream = new ObjectInputStream(connection.getInputStream());
            est = new Thread(eventSender);
            est.start();
            startEventReceiverThread();
        } catch (IOException e) {
            e.printStackTrace();
        }
        eventReplayer = new EventReplayer(area, dec, log);
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
        if(callback.getTime() < event.getTimestamp()) {
            System.out.println(callback.getTime() + " < " + event.getTimestamp());
            log.put(event.getTimestamp(), event);
            eventReplayer.replayEvent(event);
        } else if(callback.getTime() > event.getTimestamp()) {
            System.out.println(callback.getTime() + " > " + event.getTimestamp());
            SortedMap<Double, TextEvent> rollbackMap = log.tailMap(event.getTimestamp());
            callback.incTime();
            RollbackEvent rollbackEvent = new RollbackEvent(event.getOffset(), rollbackMap, callback.getTime());
            eventReplayer.replayEvent(rollbackEvent);
            updateOffsets(rollbackMap, event);
            ClusterEvent clusterEvent = new ClusterEvent(event.getOffset(), rollbackMap, 0.0);
            eventReplayer.replayEvent(clusterEvent);
        }
    }

    private void updateOffsets(SortedMap<Double, TextEvent> rollbackMap, TextEvent event) {
        if(event instanceof TextRemoveEvent) {
            for(TextEvent e: rollbackMap.values()) {
                e.setOffset(e.getOffset() - ((TextRemoveEvent) event).getLength());
            }
        } else if(event instanceof  TextInsertEvent) {
            for(TextEvent e: rollbackMap.values()) {
                e.setOffset(e.getOffset() + ((TextInsertEvent) event).getText().length());
            }
        }
    }
}
