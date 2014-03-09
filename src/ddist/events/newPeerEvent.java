package ddist.events;

import ddist.ConnectionInfo;

/**
 * Created by Benjamin on 08-03-14.
 */
public class NewPeerEvent implements Event{
    private int peerId;
    private ConnectionInfo peerAddress;
    private static final long serialVersionUID = 1L;

    public NewPeerEvent(int peerId, ConnectionInfo peerAddress) {
        this.peerId = peerId;
        this.peerAddress = peerAddress;
    }

    public ConnectionInfo getPeerAddress() {
        return peerAddress;
    }

    public int getPeerId() {
        return peerId;
    }
}
