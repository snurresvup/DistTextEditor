package ddist.events;


import ddist.ConnectionInfo;
import ddist.VectorClock;

import java.util.HashMap;

public class InitialSetupEvent implements Event {
    private String areaText;
    private int clientOffset;
    private VectorClock timestamp;
    private HashMap<Integer, ConnectionInfo> peers;
    private static final long serialVersionUID = 1L;
    private int senderId;

    public InitialSetupEvent(String areaText, int clientOffset, VectorClock timestamp, HashMap<Integer, ConnectionInfo> peers, int senderId) {
        this.areaText = areaText;
        this.clientOffset = clientOffset;
        this.timestamp = timestamp;
        this.peers = peers;
        this.senderId = senderId;
    }

    public String getAreaText() {
        return areaText;
    }

    public int getClientOffset() {
        return clientOffset;
    }
    public VectorClock getTimestamp() {
        return timestamp;
    }
    public HashMap<Integer, ConnectionInfo> getPeers() {
        return peers;
    }

    public int getSenderId() {
        return senderId;
    }
}
