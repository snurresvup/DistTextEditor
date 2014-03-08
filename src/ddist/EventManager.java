package ddist;

import ddist.events.*;
import ddist.events.text.*;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
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
    private HashMap<Double, ConnectionInfo> peers = new HashMap<>();
    private HashMap<Double, Socket> connections = new HashMap<>();
    private ArrayList<ObjectInputStream> inputStreams = new ArrayList<>();
    private HashMap<Double, HashSet<Double>> acknowledgements = new HashMap<>();
    private double numberOfPeers = 1;
    private HashSet<TextEvent> acknowledgedBySelf = new HashSet<>();


    public EventManager(JTextArea area, DocumentEventCapturer dec, CallBack time) {
        this.area = area;
        this.dec = dec;
        this.callback = time;
        eventReplayer = new EventReplayer(area, dec);
        eventSender = new EventSender(dec, this, callback);
        est = new Thread(eventSender);
        est.start();
    }

    @Override
    public void run() {
        while (!interrupted()){
            executeNonTextEvent();
        }
    }

    private void try2ExecuteTextEvent() {
        TextEvent textEvent;
        do {
            textEvent = textEvents.peek();
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
        } while (textEvents.peek() != null && textEvent != textEvents.peek());
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


    private void startEventReceiverThread(final ObjectInputStream inputStream) {
        new Thread(new Runnable() {
            boolean receiving = true;
            @Override
            public void run() {
                while (receiving){
                    Object input = null;
                    try {
                        input = inputStream.readObject();
                    } catch (IOException e) {
                        queueEvent(new DisconnectEvent());
                        receiving = false;
                        e.printStackTrace();
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
        }).start();
    }

    public synchronized void queueEvent(Event event){
        if(event instanceof TextEvent){
            textEvents.put((TextEvent)event);
            handleAcknowledgeEvent(new AcknowledgeEvent(callback.getID(), ((TextEvent) event).getTimestamp()));
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
        } else if(event instanceof JoinEvent) {
            handleJoinEvent((JoinEvent)event);
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

    private void handleDisconnectEvent() { //TODO FIX THIS
        synchronized (area){
            dec.setFilter(false);
        }
        eventSender.close();
        callback.setTitleOfWindow("Disconnected");
        callback.setDisconnect(false);
        callback.setConnect(true);
        callback.setListen(true);
    }

    private void closeConnections() {
        for(Socket socket : connections.values()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeInputStreams() {
        for(InputStream input : inputStreams){
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        callback.setTime(initEvent.getClientOffset() + Math.floor(initEvent.getTimestamp()) + 1);
        handleTextEvent(new TextInsertEvent(0, initEvent.getAreaText(), 0.0));
        establishInitialConnections(initEvent.getPeers());
        setNumberOfPeers(initEvent.getPeers());
    }

    private void establishInitialConnections(HashMap<Double, ConnectionInfo> peers) {
        for(double id : peers.keySet()) {
            try {
                ConnectionInfo connectionInfo = peers.get(id);
                Socket socket = new Socket(connectionInfo.getInetAddress(), connectionInfo.getPort());
                connections.put(id, socket);
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                inputStreams.add(inputStream);
                eventSender.addPeer(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void setNumberOfPeers(HashMap<Double, ConnectionInfo> peers) {
        numberOfPeers = peers.size() + 1;
    }

    private void handleConnectionEvent(ConnectionEvent event) { //TODO multiple peers connecting to different listeners simultaneously
        Socket socket = event.getSocket();
        InetAddress ip = socket.getInetAddress();
        int port = socket.getPort();

        double id4Client = numberOfPeers / 10000;
        System.out.println("nop: " + numberOfPeers);
        System.out.println("id4c: " + id4Client);
        connections.put(id4Client, event.getSocket());
        numberOfPeers++;
        try {
            eventSender.addPeer(event.getSocket());
            ObjectInputStream inputStream = new ObjectInputStream(event.getSocket().getInputStream());
            inputStreams.add(inputStream);
            startEventReceiverThread(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        synchronized (area) {
            dec.setFilter(true);
        }
        eventSender.queueEvent(
                new InitialSetupEvent(area.getText(), id4Client, callback.getTimestamp(), (HashMap<Double, ConnectionInfo>) peers.clone()));
        peers.put(id4Client, new ConnectionInfo(ip, port));
    }

    private void handleJoinEvent(JoinEvent event) {
        Socket socket = event.getSocket();
        try {
            eventSender.addPeer(socket);
            ObjectInputStream inputStream = new ObjectInputStream(event.getSocket().getInputStream());
            inputStreams.add(inputStream);
            startEventReceiverThread(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        synchronized (area) {
            dec.setFilter(true);
        }
        callback.startListeningThread(0);
        callback.setConnect(false);
        callback.setDisconnect(true);
        callback.setListen(false);
        callback.setStopListening(false);
    }

    private void handleTextEvent(TextEvent event) {
        synchronized (area) {
            callback.setTime(Math.max(Math.floor(event.getTimestamp()), Math.floor(callback.getTime())) + callback.getID() + 1);
        }
        System.out.println("Handling text event: " + event.getTimestamp() + ", new timestamp: " + callback.getTime());
        eventReplayer.replayEvent(event);
    }

    public void disconnected() {
        closeInputStreams();
        closeConnections();
        callback.setTitleOfWindow("Disconnected");
        numberOfPeers = 1;
    }
}
