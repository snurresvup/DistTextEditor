package ddist.events;


public class RemovePeerEvent implements Event{
    private double peerId;

    public RemovePeerEvent(double peerId) {
        this.peerId = peerId;
    }

    public double getPeerId() {
        return peerId;
    }
}
