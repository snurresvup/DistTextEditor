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
                // Our changed implementation switches between two states, depending of wheter we are connected or not.
                MyTextEvent mte;
                if (!replayingRemoteEvents) {
                    mte = dec.take();
                } else {
                    mte = receivedEvents.take();
                }

                // Begin: This code is not changed
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
                // End: This code is not changed

            } catch (InterruptedException ie) {
                useRemoteOrLocalEvents();
            }
        }
    }

    /**
     * This method sets the state according to the connected status of the text editor
     */
    private void useRemoteOrLocalEvents() {
        if (dte.isConnected()) {
            // If the text editor is connected we start receiving and sending events. Events in the queue of the
            // document event capturer, are sent through an object stream. Events are received and put in the
            // receivedEvents queue, and afterwards replayed as usual.
            replayingRemoteEvents = true;
            sender = new Thread(new EventSender(dte.getSocket(), dec, this));
            sender.start();
            receiver = new Thread(new EventReceiver(dte.getSocket(), receivedEvents, this));
            receiver.start();
            run();
        } else {
            // Here we simply switch back to the normal mode.
            replayingRemoteEvents = false;
            dte.getArea2().setText("");
            run();
        }
    }

    /**
     * This method is called when an exception is raised from the object input stream because of a closed.
      */
    public void handleClosedSocket() {
        // First we ensure that the sender thread is terminated.
        sender.interrupt();
        if (dte.isConnected()) {
            // We change the connected state of the text editor, then we update the event replayer to reflect the change
            dte.setConnected(false);
            dte.interruptEventReplayer();
        }
        if (dte.getServer() != null) {
            // In the case that we are acting as server we return to the listening state.
            dte.startConnectionListener();
        } else {
            // If we are a client we reset the text area and set the title to disconnected.
            dte.resetText();
            dte.setTitle("Disconnected");
        }
    }
}
