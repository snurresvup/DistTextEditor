package ddist.events.text;


import ddist.VectorClock;

public class TextInsertEvent extends TextEvent {

    private static final long serialVersionUID = 1L;
    private String text;
    private VectorClock timestamp;
	
	public TextInsertEvent(int offset, String text, VectorClock time) {
		super(offset, time);
		this.text = text;
        this.timestamp = time;
	}

	public String getText() { return text; }

    public VectorClock getTimestamp() {
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

