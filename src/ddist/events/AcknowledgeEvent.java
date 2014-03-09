package ddist.events;

import ddist.VectorClock;

public class AcknowledgeEvent implements Event {
    private int senderId;
    private VectorClock eventId;
    private static final long serialVersionUID = 1L;


    public AcknowledgeEvent(int senderId, VectorClock eventId) {
        this.eventId = eventId;
        this.senderId = senderId;
    }

    public int getSenderId() {
        return senderId;
    }

    public VectorClock getEventId() {
        return eventId;
    }
}
