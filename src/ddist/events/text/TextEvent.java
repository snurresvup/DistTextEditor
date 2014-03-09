package ddist.events.text;

import ddist.VectorClock;
import ddist.events.Event;
import java.lang.Comparable;


public abstract class TextEvent implements Event, Comparable<TextEvent> {

    private static final long serialVersionUID = 1L;

    public void setOffset(int offset) {
        this.offset = offset;
    }

    private int offset;
    private VectorClock timestamp;

    TextEvent(int offset, VectorClock timestamp) {
        this.offset = offset;
        this.timestamp = timestamp;
    }

    public VectorClock getTimestamp() {return timestamp;}

    public int getOffset() {
        return offset;
    }

    @Override
    public int compareTo(TextEvent event){
        return getTimestamp().compareTo(event.getTimestamp());
    }
}
