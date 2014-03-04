package ddist.events.text;

public class TextRemoveEvent extends TextEvent {

    private int length;
    private String text;
    private double timestamp;

    public TextRemoveEvent(int offset, int length, Double time) {
        super(offset, time);
        this.length = length;
        this.timestamp = time;
    }

    public int getLength() {
        return length;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getTimestamp() {
        return timestamp;
    }
}
