package ddist.events.text;

import java.math.BigInteger;

public class TextRemoveEvent extends TextEvent {

	private int length;
	
	public TextRemoveEvent(int offset, int length, Double time) {
		super(offset, time);
		this.length = length;
	}
	
	public int getLength() { return length; }
}
