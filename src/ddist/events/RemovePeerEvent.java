package ddist.events;


public class RemovePeerEvent implements Event{
    private double peerId;
    private static final long serialVersionUID = 1L;

    public RemovePeerEvent(double peerId) {
        this.peerId = peerId;
    }

    public double getPeerId() {
        return peerId;
    }
}
