package ddist.events.text;

import ddist.VectorClock;

public class TextRemoveEvent extends TextEvent {

    private static final long serialVersionUID = 1L;
    private int length;
    private VectorClock timestamp;

    public TextRemoveEvent(int offset, int length, VectorClock time) {
        super(offset, time);
        this.length = length;
        this.timestamp = time;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "TextRemoveEvent{" +
                "timestamp=" + timestamp +
                '}';
    }

    public VectorClock getTimestamp() {
        return timestamp;
    }
}
