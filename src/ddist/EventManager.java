package ddist;

import ddist.events.*;
import ddist.events.text.*;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import static java.lang.Thread.interrupted;

public class EventManager implements Runnable {
    private PriorityBlockingQueue<TextEvent> textEvents = new PriorityBlockingQueue<>();
    private LinkedBlockingQueue<Event> nonTextEvents = new LinkedBlockingQueue<>();

    private EventSender eventSender;
    private Thread est;
    private EventReplayer eventReplayer;
    private final JTextArea area;
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


    private void startEventReceiverThread(final ObjectInputStream input, final double remoteId) {
        final ObjectInputStream inputStream = input;
        new Thread(new Runnable() {
            boolean receiving = true;
            @Override
            public void run() {
                while (receiving){
                    Object input = null;
                    try {
                        input = inputStream.readObject();
                    } catch (IOException e) {
                        queueEvent(new RemovePeerEvent(remoteId));
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
                        }else if(input instanceof NewPeerEvent) {
                            System.out.println("Received NewPeerEvent at " + callback.getID() + " from peer: " + ((NewPeerEvent) input).getPeerId());
                        }else if(input instanceof InitialSetupEvent) {
                            System.out.println("Received InitialSetupEvent at " + ((InitialSetupEvent) input).getClientOffset() + "containing: " + ((InitialSetupEvent) input).getPeers());
                        }else if(input instanceof RemovePeerEvent) {
                            System.out.println("Received RemovePeerEvent at " + callback.getID() + ". Peer to remove: " + ((RemovePeerEvent) input).getPeerId());
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
        } else if(event instanceof NewPeerEvent) {
            handleNewPeerEvent((NewPeerEvent)event);
        } else if(event instanceof RemovePeerEvent) {
            handleRemovePeerEvent((RemovePeerEvent)event);
        }
    }

    private void handleRemovePeerEvent(RemovePeerEvent event) {
        peers.remove(event.getPeerId());
        connections.remove(event.getPeerId());
        eventSender.removePeer(event.getPeerId());
        for(Double e : acknowledgements.keySet()) {
            acknowledgements.get(e).remove(event.getPeerId());
        }
        numberOfPeers--;
    }

    private void handleNewPeerEvent(NewPeerEvent event) {
        // The peer which was not connected by the new peer receives a NewPeerEvent, which contains the ID and the
        // ConnectionInfo of the new peer.
        peers.put(event.getPeerId(),event.getPeerAddress());
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
        eventSender.queueEvent(new RemovePeerEvent(callback.getID()));
        synchronized (area){
            dec.setFilter(false);
        }
        eventSender.close();
        callback.setTitleOfWindow("Disconnected");
        callback.setDisconnect(false);
        callback.setConnect(true);
        callback.setListen(true);
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
        // When the two peers are initially connected, we need to tell the new peer about the rest of the network, and
        // setup the text area. We also need to set our ID and the current time, which is done below.
        if(numberOfPeers == 1){
            clearTextArea();
            callback.setID(initEvent.getClientOffset());
            callback.setTime(initEvent.getClientOffset() + Math.floor(initEvent.getTimestamp()) + 1);
            handleTextEvent(new TextInsertEvent(0, initEvent.getAreaText(), 0.0));
            establishInitialConnections(initEvent.getPeers());
            setNumberOfPeers(initEvent.getPeers());
        }
    }

    private void establishInitialConnections(HashMap<Double, ConnectionInfo> peers) {
        // Runs through all the peers of the peer we just connected to. That is all, minus himself. Thus when the first two
        // peers connect to each other, the peer keyset is empty is thus the for loop below does nothing. However, when
        // a third peer connects, there's a single peer in either of the peer's peer keyset. Since we did not connect to him
        // directly when we connected to the first peer, this is handled here.
        for(double id : peers.keySet()) {
            try {
                ConnectionInfo connectionInfo = peers.get(id);
                System.out.println("Establishing connection to: " + connectionInfo.getInetAddress() + ":" + connectionInfo.getPort());
                Socket socket = new Socket(connectionInfo.getInetAddress(), connectionInfo.getPort());
                connections.put(id, socket);
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                inputStreams.add(inputStream);
                startEventReceiverThread(inputStream, id);
                eventSender.addPeer(id, socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            eventSender.queueEvent(new NewPeerEvent(callback.getID(), new ConnectionInfo(InetAddress.getLocalHost(), callback.getPort())));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private void setNumberOfPeers(HashMap<Double, ConnectionInfo> peers) {
        // When we receive an initial setup event the peers contained in the HashMap of the sender is everyone in his
        // network minus himself. Thus, we're going to add 2 because the total amount of peers is going to be 2 higher
        // than what he knows when we connect to him.
        numberOfPeers = peers.size() + 2;
    }

    private void handleConnectionEvent(ConnectionEvent event) { //TODO multiple peers connecting to different listeners simultaneously
        // When a client connects to a peer in the network, it receives a ConnectionEvent. In this method we handle the
        // initial setup of id's for the new peer, and telling him what the state of the network / program is.
        double id4Client = numberOfPeers / 10000;
        connections.put(id4Client, event.getSocket());
        numberOfPeers++;
        try {
            // Adds the peer to our outgoing/ingoing streams and starts listening for events on the inputstream.
            eventSender.addPeer(id4Client, event.getSocket());
            ObjectInputStream inputStream = new ObjectInputStream(event.getSocket().getInputStream());
            inputStreams.add(inputStream);
            startEventReceiverThread(inputStream, id4Client);
        } catch (IOException e) {
            e.printStackTrace();
        }
        synchronized (area) {
            dec.setFilter(true);
        }

        eventSender.sendEventToPeer(
                new InitialSetupEvent(area.getText(), id4Client, callback.getTimestamp(), (HashMap<Double, ConnectionInfo>) peers.clone()), id4Client);
        System.out.println("id4client" + id4Client);
        //TODO set noget på standby maybe
    }

    private void handleJoinEvent(JoinEvent event) {
        // When we connect to a peer we queue a local JoinEvent. This adds the input/output streams accordingly to the peer
        // that we are now connected to.
        Socket socket = event.getSocket();
        try {
            eventSender.addPeer(12.3, socket); //TODO fix
            ObjectInputStream inputStream = new ObjectInputStream(event.getSocket().getInputStream());
            inputStreams.add(inputStream);
            startEventReceiverThread(inputStream, 12.3);
        } catch (IOException e) {
            e.printStackTrace();
        }
        synchronized (area) {
            dec.setFilter(true);
        }
        int port = 0;
        try {
            ServerSocket temp = new ServerSocket(0);
            port = temp.getLocalPort();
            temp.close();
            temp = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        callback.startListeningThread(port);
        callback.setTitleOfWindow("Connected! Listening on: " + callback.getIp() + ":" + port);
        callback.setConnect(false);
        callback.setDisconnect(true);
        callback.setListen(false);
    }

    private void handleTextEvent(TextEvent event) {
        synchronized (area) {
            callback.setTime(Math.max(Math.floor(event.getTimestamp()), Math.floor(callback.getTime())) + callback.getID() + 1);
        }
        System.out.println("Handling text event: " + event.getTimestamp() + ", new timestamp: " + callback.getTime());
        eventReplayer.replayEvent(event);
    }

    public void disconnect() {
        eventSender.queueEvent(new RemovePeerEvent(callback.getID()));
        closeInputStreams();
        callback.setTitleOfWindow("Disconnected");
        numberOfPeers = 1;
    }
}