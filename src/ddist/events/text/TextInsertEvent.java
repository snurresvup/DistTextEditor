package ddist.events.text;

import java.math.BigInteger;

/**
 * 
 * @author Jesper Buus Nielsen
 *
 */
public class TextInsertEvent extends TextEvent {

	private String text;
	
	public TextInsertEvent(int offset, String text, Double time) {
		super(offset, time);
		this.text = text;
	}

	public String getText() { return text; }
}

