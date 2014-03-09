package ddist.events;


public class RemovePeerEvent implements Event{
    private int peerId;
    private static final long serialVersionUID = 1L;

    public RemovePeerEvent(int peerId) {
        this.peerId = peerId;
    }

    public int getPeerId() {
        return peerId;
    }
}
