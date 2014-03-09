package ddist;

public class EventReceiverThread extends Thread {
    private double remoteId;

    public EventReceiverThread(Runnable runnable, double remoteId) {
        super(runnable);
        this.remoteId = remoteId;
    }

    public double getRemoteId() {
        return remoteId;
    }
}
