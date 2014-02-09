import javax.swing.JTextArea;
import java.awt.EventQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
    private DistributedTextEditor dte;
    private boolean replayingRemoteEvents;
    private LinkedBlockingQueue<MyTextEvent> receivedEvents;
    private Thread receiver;
    private Thread sender;

    public EventReplayer(DocumentEventCapturer dec, JTextArea area, DistributedTextEditor dte) {
        this.dec = dec;
        this.area = area;
        this.dte = dte;
        replayingRemoteEvents = false;
        receivedEvents = new LinkedBlockingQueue<MyTextEvent>();
    }

    public void run() {
        while (true) {
            try {
                MyTextEvent mte;
                if (!replayingRemoteEvents) {
                    mte = dec.take();
                } else {
                    mte = receivedEvents.take();
                }
                if (mte instanceof TextInsertEvent) {
                    final TextInsertEvent tie = (TextInsertEvent)mte;
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
                } else if (mte instanceof TextRemoveEvent) {
                    final TextRemoveEvent tre = (TextRemoveEvent)mte;
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
            } catch (InterruptedException ie) {
                useRemoteOrLocalEvents();
            }
        }
    }

    private void useRemoteOrLocalEvents() {
        if (dte.isConnected()) {
            replayingRemoteEvents = true;
            sender = new Thread(new EventSender(dte.getSocket(), dec, this));
            sender.start();
            receiver = new Thread(new EventReceiver(dte.getSocket(), receivedEvents, this));
            receiver.start();
            run();
        } else {
            replayingRemoteEvents = false;
            dte.getArea2().setText("");
            run();
        }
    }

    public void handleClosedSocket() {
        sender.interrupt();
        if (dte.isConnected()) {
            dte.setConnected(false);
            dte.interruptEventReplayer();
        }
        if (dte.getServer() != null) {
            dte.startConnectionListener();
        } else {
            dte.resetText();
            dte.setTitle("Disconnected");
        }
    }
}
