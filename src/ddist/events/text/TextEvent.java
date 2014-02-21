package ddist.events.text;

import ddist.events.Event;

import java.math.BigInteger;

/**
 * @author Jesper Buus Nielsen
 */
public abstract class TextEvent implements Event{
    private double timestamp;
    private int offset;

    TextEvent(int offset, Double timestamp) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }
}
