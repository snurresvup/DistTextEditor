package ddist.events.text;

import ddist.events.Event;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author Jesper Buus Nielsen
 */
public abstract class TextEvent implements Event {
    private double timestamp;

    public void setOffset(int offset) {
        this.offset = offset;
    }

    private int offset;

    TextEvent(int offset, Double timestamp) {
        this.offset = offset;
        this.timestamp = timestamp;
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
