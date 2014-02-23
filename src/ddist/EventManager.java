package ddist;

import ddist.events.*;
import ddist.events.text.*;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.interrupted;

public class EventManager implements Runnable {

    private static final Double UNUSED_TIMESTAMP = 0.0;
    private static final double TIME_OFFSET = 0.0001 ;
    private LinkedBlockingQueue<Event> events = new LinkedBlockingQueue<>();

    private EventSender eventSender;
    private Thread est;
    private EventReplayer eventReplayer;
    private JTextArea area;
    private DocumentEventCapturer dec;
    private CallBack callback;
    private SortedMap<Double, TextEvent> log;
    private Socket connection;
    private ObjectInputStream inputStream;
    private double currentClientOffset = 0;


    public EventManager(JTextArea area, DocumentEventCapturer dec, CallBack time) {
        this.area = area;
        this.dec = dec;
        this.callback = time;
        log = callback.getLog();
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
            TextEvent textEvent = (TextEvent)event;
            if(textEvent instanceof TextInsertEvent){
                System.out.println("InsertEvent");
                System.out.println(((TextInsertEvent)textEvent).getText());
            }else if(textEvent instanceof TextRemoveEvent){
                System.out.println("RemoveEvent");
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
            System.out.println("discevent");
            handleDisconnectEvent();
        } else if(event instanceof RollbackEvent) {
            RollbackEvent rollbackEvent = (RollbackEvent) event;
            handleRollbackEvent(rollbackEvent);
        }
    }

    private void handleRollbackEvent(RollbackEvent rollbackEvent) {
        for (TextEvent te : rollbackEvent.getEvents()) {
            log.put(te.getTimestamp(), te);
            eventReplayer.replayEvent(te);
        }
    }

    private void handleDisconnectEvent() {
        dec.setFilter(false);
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
            callback.startListeningThread();
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
        queueEvent(new ClearTextEvent());
        callback.setID(initEvent.getClientOffset() - initEvent.getTimestamp());
        callback.setTime(initEvent.getClientOffset());
        queueEvent(new TextInsertEvent(0, initEvent.getAreaText(), 0.0));
    }

    private void handleConnectionEvent(ConnectionEvent event) {
        connection = event.getSocket();
        events.clear();
        try {
            eventSender = new EventSender(dec, connection);
            inputStream = new ObjectInputStream(connection.getInputStream());
            est = new Thread(eventSender);
            est.start();
            startEventReceiverThread();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dec.setFilter(true);
        eventReplayer = new EventReplayer(area, dec, log);
        callback.setTitleOfWindow("Connected!!!");
        callback.setConnect(false);
        callback.setDisconnect(true);
        callback.setListen(false);
        callback.setStopListening(false);
        if (callback.isServer()) {
            callback.setID(0);
            currentClientOffset += TIME_OFFSET;
            eventSender.queueEvent(
                    new InitialSetupEvent(area.getText(), currentClientOffset, callback.getTime()));
        }
    }

    private void handleTextEvent(TextEvent event) {
        if(callback.getTime() < event.getTimestamp()){
            log.put(event.getTimestamp(), event);
            callback.setTime(Math.floor(event.getTimestamp()) + callback.getID());
            eventReplayer.replayEvent(event);
        } else if (callback.getTime() > event.getTimestamp()) {
            TreeMap<Double, TextEvent> rollbackMap = new TreeMap<>(log.tailMap(event.getTimestamp()));
            ArrayList<TextEvent> rollbackApplyRollforward = new ArrayList<>();
            for (TextEvent te : rollbackMap.descendingMap().values()) {
                rollbackApplyRollforward.add(invertEvent(te));
            }
            rollbackApplyRollforward.add(event);
            updateOffsets(rollbackMap, event);
            rollbackApplyRollforward.addAll(rollbackMap.values());
            queueEvent(new RollbackEvent(rollbackApplyRollforward));
        }
    }

    private TextEvent invertEvent(TextEvent textEvent) {
        if(textEvent instanceof TextInsertEvent) {
            TextInsertEvent insertEvent = (TextInsertEvent)textEvent;
            return new TextRemoveEvent(insertEvent.getOffset(), insertEvent.getText().length(), UNUSED_TIMESTAMP);
        } else if(textEvent instanceof TextRemoveEvent) {
            TextRemoveEvent removeEvent = (TextRemoveEvent)textEvent;
            return new TextInsertEvent(removeEvent.getOffset(), removeEvent.getText(), UNUSED_TIMESTAMP);
        } else {
            throw new IllegalArgumentException("Unable to rollback a rollback-event :(");
        }
    }

    private void updateOffsets(SortedMap<Double, TextEvent> rollbackMap, TextEvent event) {
        if (event instanceof TextRemoveEvent) {
            for (TextEvent e: rollbackMap.values()) {
                if (e.getOffset() >= event.getOffset()){
                    e.setOffset(e.getOffset() - ((TextRemoveEvent) event).getLength());
                }
            }
        } else if (event instanceof  TextInsertEvent) {
            for(TextEvent e: rollbackMap.values()) {
                if(e.getOffset() >= event.getOffset()){
                    e.setOffset(e.getOffset() + ((TextInsertEvent) event).getText().length());
                }
            }
        }
    }

    public void disconnected() {
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
