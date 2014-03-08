package ddist.events;

import ddist.ConnectionInfo;

/**
 * Created by Benjamin on 08-03-14.
 */
public class NewPeerEvent implements Event{
    private double peerId;
    private ConnectionInfo peerAddress;

    public NewPeerEvent(double peerId, ConnectionInfo peerAddress) {
        this.peerId = peerId;
        this.peerAddress = peerAddress;
    }

    public ConnectionInfo getPeerAddress() {
        return peerAddress;
    }

    public double getPeerId() {
        return peerId;
    }
}
