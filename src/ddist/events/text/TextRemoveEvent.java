package ddist.events.text;

import java.math.BigInteger;

public class TextRemoveEvent extends MyTextEvent {

	private int length;
	
	public TextRemoveEvent(int offset, int length, BigInteger time) {
		super(offset, time);
		this.length = length;
	}
	
	public int getLength() { return length; }
}
