package ddist.events;


import ddist.ConnectionInfo;

import java.util.HashMap;

public class InitialSetupEvent implements Event {
    private String areaText;
    private double clientOffset;
    private double timestamp;
    private HashMap<Double, ConnectionInfo> peers;

    public InitialSetupEvent(String areaText, double clientOffset, double timestamp, HashMap<Double, ConnectionInfo> peers) {
        this.areaText = areaText;
        this.clientOffset = clientOffset;
        this.timestamp = timestamp;
        this.peers = peers;
    }

    public String getAreaText() {
        return areaText;
    }

    public double getClientOffset() {
        return clientOffset;
    }
    public double getTimestamp() {
        return timestamp;
    }
    public HashMap<Double, ConnectionInfo> getPeers() {
        return peers;
    }
}
