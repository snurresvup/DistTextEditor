package ddist.events.text;

import ddist.events.Event;

import java.io.Serializable;
import java.math.BigInteger;


public abstract class TextEvent implements Event {

    public void setOffset(int offset) {
        this.offset = offset;
    }

    private int offset;
    private double timestamp;

    TextEvent(int offset, double timestamp) {
        this.offset = offset;
        this.timestamp = timestamp;
    }

    public double getTimestamp() {return timestamp;}

    public int getOffset() {
        return offset;
    }
}
