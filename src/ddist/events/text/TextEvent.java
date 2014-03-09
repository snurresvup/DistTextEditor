package ddist.events.text;

import ddist.events.Event;
import java.lang.Comparable;


public abstract class TextEvent implements Event, Comparable<TextEvent> {

    private static final long serialVersionUID = 1L;

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

    @Override
    public int compareTo(TextEvent event){
        if(getTimestamp()-event.getTimestamp() > 0){
            return 1;
        } else if (getTimestamp()-event.getTimestamp() <0){
            return -1;
        }
        return 0;
    }
}
