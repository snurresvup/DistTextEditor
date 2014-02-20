package ddist.events.text;

/**
 * 
 * @author Jesper Buus Nielsen
 *
 */
public class MyTextEvent {
	MyTextEvent(int offset) {
		this.offset = offset;
	}
	private int offset;
	public int getOffset() { return offset; }
}
