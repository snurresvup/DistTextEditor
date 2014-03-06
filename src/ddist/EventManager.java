package ddist;

import ddist.events.*;
import ddist.events.text.*;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import static java.lang.Thread.interrupted;

public class EventManager implements Runnable {

    private static final double TIME_OFFSET = 0.0001 ;
    private PriorityBlockingQueue<TextEvent> textEvents = new PriorityBlockingQueue<>();
    private LinkedBlockingQueue<Event> nonTextEvents = new LinkedBlockingQueue<>();

    private EventSender eventSender;
    private Thread est;
    private EventReplayer eventReplayer;
    private JTextArea area;
    private DocumentEventCapturer dec;
    private CallBack callback;
    private Socket connection;
    private ObjectInputStream inputStream;
    private double currentClientOffset = 0;
    private HashMap<Double, HashSet<Double>> acknowledgements = new HashMap<>();
    private int numberOfPeers = 2;//TODO
    private HashSet<TextEvent> acknowledgedBySelf = new HashSet<>();


    public EventManager(JTextArea area, DocumentEventCapturer dec, CallBack time) {
        this.area = area;
        this.dec = dec;
        this.callback = time;
    }

    @Override
    public void run() {
        while (!interrupted()){
            executeNonTextEvent();
        }
    }

    private void try2ExecuteTextEvent() {
        TextEvent textEvent = textEvents.peek();
        if(textEvent != null) {
            sendAcknowledgement(textEvent);
            if(isAcknowledged(textEvent)){
                try {
                    textEvents.take();
                    acknowledgements.remove(textEvent.getTimestamp());
                    acknowledgedBySelf.remove(textEvent);
                    dec.markEventAsExecuted(textEvent);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handleTextEvent(textEvent);
            }
        }
    }

    private void executeNonTextEvent() {
        try {
            Event event = nonTextEvents.take();
            handleNonTextEvent(event);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean isAcknowledged(TextEvent event) {
        double eventId = Math.ceil(event.getTimestamp() * 10000) / 10000;
        if(acknowledgements.get(eventId) != null){
            if(acknowledgements.get(eventId).size() == numberOfPeers){
                return true;
            }
        }
        return false;
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
                            if(input instanceof AcknowledgeEvent){
                                System.out.println("Received acknowledge at " + callback.getID() + " on event: " + ((AcknowledgeEvent)input).getEventId() + " \n" +
                                        "From: " + ((AcknowledgeEvent)input).getSenderId());
                            }else if(input instanceof TextInsertEvent) {
                                System.out.println("Received TextInsertEvent " + ((TextInsertEvent)input).getTimestamp() + "");
                            }
                            queueEvent((Event) input);
                        }
                    }
                }
            }
        }).start();
    }

    public void queueEvent(Event event){
        if(event instanceof TextEvent){
            textEvents.put((TextEvent)event);
            handleAcknowledgeEvent(new AcknowledgeEvent(callback.getID(), ((TextEvent) event).getTimestamp()));
            //sendAcknowledgement((TextEvent) event);
        } else {
            try {
                nonTextEvents.put(event);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendAcknowledgement(TextEvent event) {
        if(!acknowledgedBySelf.contains(event)){
            eventSender.queueEvent(new AcknowledgeEvent(callback.getID(), event.getTimestamp()));
            acknowledgedBySelf.add(event);
        }
    }

    private void handleNonTextEvent(Event event) {
        if(event instanceof ConnectionEvent) {
            ConnectionEvent connectionEvent = (ConnectionEvent) event;
            handleConnectionEvent(connectionEvent);
        } else if(event instanceof InitialSetupEvent) {
            InitialSetupEvent initEvent = (InitialSetupEvent)event;
            handleInitialSetupEvent(initEvent);
        } else if(event instanceof ClearTextEvent) {
            clearTextArea();
        } else if(event instanceof DisconnectEvent) {
            handleDisconnectEvent();
        } else if(event instanceof AcknowledgeEvent) {
            handleAcknowledgeEvent((AcknowledgeEvent)event);
        }
    }

    private synchronized void handleAcknowledgeEvent(AcknowledgeEvent event) {
        double eventId = Math.ceil(event.getEventId() * 10000) / 10000;
        double senderId = Math.ceil(event.getSenderId() * 10000) / 10000;
        if(acknowledgements.containsKey(eventId)){
            acknowledgements.get(eventId).add(senderId);
        } else {
            HashSet<Double> ids = new HashSet<>();
            ids.add(senderId);
            acknowledgements.put(eventId, ids);
        }
        try2ExecuteTextEvent();
    }

    private void handleDisconnectEvent() {
        synchronized (area){
            dec.setFilter(false);
        }
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
        synchronized (area) {
            dec.setFilter(false);
            area.setText("");
            dec.setFilter(true);
        }
    }

    private void handleInitialSetupEvent(InitialSetupEvent initEvent) {
        clearTextArea();
        callback.setID(initEvent.getClientOffset());
        callback.setTime(initEvent.getClientOffset() + Math.floor(initEvent.getTimestamp()));
        handleTextEvent(new TextInsertEvent(0, initEvent.getAreaText(), 0.0));
    }

    private void handleConnectionEvent(ConnectionEvent event) {
        connection = event.getSocket();
        try {
            eventSender = new EventSender(dec, connection, this, callback);
            inputStream = new ObjectInputStream(connection.getInputStream());
            est = new Thread(eventSender);
            est.start();
            startEventReceiverThread();
        } catch (IOException e) {
            e.printStackTrace();
        }
        synchronized (area) {
            dec.setFilter(true);
        }
        eventReplayer = new EventReplayer(area, dec);
        callback.setTitleOfWindow("Connected!!!");
        callback.setConnect(false);
        callback.setDisconnect(true);
        callback.setListen(false);
        callback.setStopListening(false);
        if (callback.isServer()) {
            callback.setID(0);
            currentClientOffset += TIME_OFFSET;
            eventSender.queueEvent(
                    new InitialSetupEvent(area.getText(), currentClientOffset, callback.getTimestamp()));
        }
    }

    private void handleTextEvent(TextEvent event) {
        synchronized (area) {
            callback.setTime(Math.max(Math.floor(event.getTimestamp()), Math.floor(callback.getTime())) + callback.getID() + 1);
        }
        System.out.println("Handeling text event: " + event.getTimestamp() + ", new timestamp: " + callback.getTime());
        eventReplayer.replayEvent(event);
    }

    public void disconnected() {
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
