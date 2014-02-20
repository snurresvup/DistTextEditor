package ddist.events.text;

import java.math.BigInteger;

/**
 * @author Jesper Buus Nielsen
 */
public abstract class MyTextEvent {
    private BigInteger timestamp;
    private int offset;

    MyTextEvent(int offset, BigInteger timestamp) {
        this.offset = offset;

    }

    public int getOffset() {
        return offset;
    }

    public BigInteger getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(BigInteger timestamp) {
        this.timestamp = timestamp;
    }
}
