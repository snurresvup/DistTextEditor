package ddist.events.text;

public class TextRemoveEvent extends TextEvent {

    private int length;
    private double timestamp;

    public TextRemoveEvent(int offset, int length, Double time) {
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

    public double getTimestamp() {
        return timestamp;
    }
}
