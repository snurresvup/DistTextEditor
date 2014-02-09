import java.io.Serializable;

/**
 * 
 * @author Jesper Buus Nielsen
 *
 */
// We made this class implement Serializable, so that we could send its object through an object stream
public class MyTextEvent implements Serializable {
	MyTextEvent(int offset) {
		this.offset = offset;
	}
	private int offset;
	int getOffset() { return offset; }
}
