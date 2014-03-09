package ddist;

import ddist.events.*;
import ddist.events.text.*;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import static java.lang.Thread.interrupted;

public class EventManager implements Runnable {
    public static final double TIME_OFFSET = 0.0001;
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
    private double currentClientOffset;


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
        // This is one of the rare ocations where the do-while loop makes perfectly sense. We have a loop that we want executed at least once but possibly more than once.
        do {
            // We look at the first event in the queue. We must also do a null check because peek() returns null if the queue is empty.
            textEvent = textEvents.peek();
            if(textEvent != null) {
                // The sendAcknowledgement method ensures that an acknowledgement is sent to every other peer.
                sendAcknowledgement(textEvent);
                // The isAcknowledged method checks if we have recorded acknowledgements from every peer on the text event it is give, it returns true if that is the case.
                if(isAcknowledged(textEvent)){
                    // We are now free to execute the event
                    try {
                        // First we do some clean up, removing the event from places where we had it recorded.
                        textEvents.take();
                        acknowledgements.remove(textEvent.getTimestamp());
                        acknowledgedBySelf.remove(textEvent);
                        dec.markEventAsExecuted(textEvent);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // We execute the event!! YAY!
                    handleTextEvent(textEvent);
                }
            }
            // The next element in the queue might have enough acknowledgements, therefore we continue the loop if the event was executed and the queue is not empty.
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
            double realId = remoteId;
            boolean receiving = true;
            @Override
            public void run() {
                while (receiving){
                    Object input = null;
                    try {
                        input = inputStream.readObject();
                        if(input instanceof InitialSetupEvent && realId == 12.3) {
                            realId = ((InitialSetupEvent) input).getTimestamp() - Math.floor(((InitialSetupEvent) input).getTimestamp());
                            realId = Math.ceil(realId * 10000)/10000;
                            System.out.println("new id on receiver thread = " + realId);
                        }
                    } catch (EOFException e) {
                        handleRemovePeerEvent(new RemovePeerEvent(realId));
                        receiving = false;
                        e.printStackTrace();
                        break;
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException io){
                        handleRemovePeerEvent(new RemovePeerEvent(realId));
                        receiving = false;
                        io.printStackTrace();
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

    // We differentiate between text events and non-text events
    public synchronized void queueEvent(Event event){
        if(event instanceof TextEvent){
            // If the event is a text event we add it to the text event queue
            textEvents.put((TextEvent)event);
            // We send an acknowledgement to ourself for the event. This also triggers a check to see if we can execute the foremost text event in the queue
            handleAcknowledgeEvent(new AcknowledgeEvent(callback.getID(), ((TextEvent) event).getTimestamp()));
        } else {
            // else we add the event to the non-text event queue
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
        System.out.println("Remove peer " + event.getPeerId());
        peers.remove(event.getPeerId());
        connections.remove(event.getPeerId());
        eventSender.removePeer(event.getPeerId());
        for(Double e : acknowledgements.keySet()) {
            acknowledgements.get(e).remove(event.getPeerId());
        }
        if(numberOfPeers > 1) {
            numberOfPeers--;
        }
    }

    private void handleNewPeerEvent(NewPeerEvent event) {
        // The peer which was not connected by the new peer receives a NewPeerEvent, which contains the ID and the
        // ConnectionInfo of the new peer.
        peers.put(event.getPeerId(),event.getPeerAddress());
        currentClientOffset = Math.max(event.getPeerId(), callback.getID());
    }

    private synchronized void handleAcknowledgeEvent(AcknowledgeEvent event) {
        // This is the id of the acknowledged event. Note that we use id as another word for timestamp.
        // We can do this because we know that the timestamps will always be unique.
        double eventId = Math.ceil(event.getEventId() * 10000) / 10000;
        // This is the id of the sender. This number is the decimal that the sender adds to integer time stamp of his events.
        // Lookup how to do total ordered multicasting to understand why we have this id.
        double senderId = Math.ceil(event.getSenderId() * 10000) / 10000;
        if(acknowledgements.containsKey(eventId)){
            // If we have already received an acknowledgement from someone on the event, we simply add the senderId to the hash set
            acknowledgements.get(eventId).add(senderId);
        } else {
            // If this is the first acknowledgement we receive on this event, we create a new hash set and add the senderId to it.
            HashSet<Double> ids = new HashSet<>();
            ids.add(senderId);
            // Then we map the eventId to the created hash set.
            acknowledgements.put(eventId, ids);
        }
        // We must now see if we can execute the foremost text event in the queue
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
            currentClientOffset = initEvent.getClientOffset();
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
                System.out.println("id of new event is ### many pears ### " + id);
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
        // When a client receives a connection from a peer, it sends a ConnectionEvent to its own eventManager. In this
        // method we handle the initial setup of id's for the new peer, and telling him what the state of the
        // network / program is.
        double id4Client = currentClientOffset + TIME_OFFSET;
        connections.put(id4Client, event.getSocket());
        numberOfPeers++;
        try {
            // Adds the peer to our outgoing/incoming streams and starts listening for events on the inputstream.
            eventSender.addPeer(id4Client, event.getSocket());
            ObjectInputStream inputStream = new ObjectInputStream(event.getSocket().getInputStream());
            inputStreams.add(inputStream);
            System.out.println("lonely pear wants to connect to me with this funny id " + id4Client);
            startEventReceiverThread(inputStream, id4Client);
        } catch (IOException e) {
            e.printStackTrace();
        }
        synchronized (area) {
            dec.setFilter(true);
        }
        try {
            eventSender.sendEventToPeer(new NewPeerEvent(callback.getID(), new ConnectionInfo(InetAddress.getLocalHost(), callback.getPort())), id4Client);
        } catch (UnknownHostException e) {
            e.printStackTrace();
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
        synchronized (area) {
            dec.setFilter(false);
        }
        eventSender.close();
        closeInputStreams();
        textEvents.clear();
        peers.clear();
        acknowledgements.clear();
        acknowledgedBySelf.clear();
        connections.clear();
        callback.setTitleOfWindow("Disconnected");
        numberOfPeers = 1;

    }
}