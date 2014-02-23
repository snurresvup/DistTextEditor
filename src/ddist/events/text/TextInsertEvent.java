package ddist.events.text;


public class TextInsertEvent extends TextEvent {

	private String text;
    private double timestamp;
	
	public TextInsertEvent(int offset, String text, Double time) {
		super(offset);
		this.text = text;
        this.timestamp = time;
	}

	public String getText() { return text; }

    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }
}

