package ddist.events.text;

import java.math.BigInteger;

public class TextRemoveEvent extends TextEvent {

	private int length;
    private String text;

    public TextRemoveEvent(int offset, int length, Double time) {
		super(offset, time);
		this.length = length;
	}
	
	public int getLength() { return length; }

    public String getText() {
        return text;
    }

    // TODO, when do we ever set this???
    public void setText(String text) {
        this.text = text;
    }
}
