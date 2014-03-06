package ddist.events;

public class AcknowledgeEvent implements Event {
    private double senderId;
    private double eventId;

    public AcknowledgeEvent(double senderId, double eventId) {
        this.eventId = eventId;
        this.senderId = senderId;
    }

    public double getSenderId() {
        return senderId;
    }

    public double getEventId() {
        return eventId;
    }
}