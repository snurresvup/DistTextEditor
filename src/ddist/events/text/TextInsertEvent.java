package ddist.events.text;


public class TextInsertEvent extends TextEvent {

    private static final long serialVersionUID = 1L;
    private String text;
    private double timestamp;
	
	public TextInsertEvent(int offset, String text, Double time) {
		super(offset, time);
		this.text = text;
        this.timestamp = time;
	}

	public String getText() { return text; }

    public double getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "TextInsertEvent{" +
                "text='" + text + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

