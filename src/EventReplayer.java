import com.javafx.tools.doclets.formats.html.SourceToHTMLConverter;

import javax.swing.JTextArea;
import java.awt.EventQueue;

import static java.lang.Thread.interrupted;

/**
 *
 * Takes the event recorded by the DocumentEventCapturer and replays
 * them in a JTextArea. The delay of 1 sec is only to make the individual
 * steps in the reply visible to humans.
 *
 * @author Jesper Buus Nielsen
 *
 */
public class EventReplayer implements Runnable {

    private DocumentEventCapturer dec;
    private JTextArea area;

    public EventReplayer(DocumentEventCapturer dec, JTextArea area) {
        this.dec = dec;
        this.area = area;
    }

    public void run() {
        while (!interrupted()) {
            try {
                MyTextEvent mte = dec.take();
                sendEvent(mte);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("I'm the thread running the EventReplayer, now I die!");
    }

    private void sendEvent(MyTextEvent mte) {


    }

    private void replayEvent(MyTextEvent event){
        if (event instanceof TextInsertEvent) {
            final TextInsertEvent tie = (TextInsertEvent)event;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        area.insert(tie.getText(), tie.getOffset());
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
                        area.replaceRange(null, tre.getOffset(), tre.getOffset()+tre.getLength());
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
