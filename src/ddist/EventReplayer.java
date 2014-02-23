package ddist;
import ddist.events.text.*;
import sun.reflect.generics.tree.Tree;

import javax.swing.JTextArea;
import java.awt.EventQueue;
import java.util.*;

/**
 *
 * Takes the event recorded by the ddist.DocumentEventCapturer and replays
 * them in a JTextArea. The delay of 1 sec is only to make the individual
 * steps in the reply visible to humans.
 *
 * @author Jesper Buus Nielsen
 *
 */
public class EventReplayer{

    private JTextArea area;
    private DocumentEventCapturer dec;
    private SortedMap<Double, TextEvent> log;

    public EventReplayer(JTextArea area, DocumentEventCapturer dec, SortedMap<Double, TextEvent> log) {
        this.area = area;
        this.dec = dec;
        this.log = log;
    }

    public synchronized void replayEvent(TextEvent event){
        if (event instanceof TextInsertEvent) {
            final TextInsertEvent tie = (TextInsertEvent)event;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        dec.setFilter(false);
                        area.insert(tie.getText(), tie.getOffset());
                        dec.setFilter(true);
                    } catch (Exception e) {
                        System.err.println(e);
				    /* We catch all axceptions, as an uncaught exception would make the
				     * EDT unwind, which is now healthy.
				     */
                    }
                }
            });
        } else if (event instanceof TextRemoveEvent) {
            final TextRemoveEvent tre = (TextRemoveEvent)event;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        dec.setFilter(false);;
                        area.replaceRange(null, tre.getOffset(), tre.getOffset()+tre.getLength());
                        dec.setFilter(true);
                    } catch (Exception e) {
                        System.err.println(e);
				    /* We catch all axceptions, as an uncaught exception would make the
				     * EDT unwind, which is now healthy.
				     */
                    }
                }
            });
        }
    }
}
