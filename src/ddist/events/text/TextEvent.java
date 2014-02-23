package ddist.events.text;

import ddist.events.Event;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author Jesper Buus Nielsen
 */
public abstract class TextEvent implements Event {

    public void setOffset(int offset) {
        this.offset = offset;
    }

    private int offset;

    TextEvent(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }
}
