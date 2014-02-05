import java.io.Serializable;

/**
 * 
 * @author Jesper Buus Nielsen
 *
 */
public class MyTextEvent implements Serializable {
	MyTextEvent(int offset) {
		this.offset = offset;
	}
	private int offset;
	int getOffset() { return offset; }
}
