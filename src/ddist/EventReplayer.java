package ddist;

import ddist.events.ConnectionEvent;
import ddist.events.text.MyTextEvent;
import ddist.events.text.TextInsertEvent;
import ddist.events.text.TextRemoveEvent;

import javax.swing.JTextArea;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

import static java.lang.Thread.interrupted;

/**
 *
 * Takes the event recorded by the ddist.DocumentEventCapturer and replays
 * them in a JTextArea. The delay of 1 sec is only to make the individual
 * steps in the reply visible to humans.
 *
 * @author Jesper Buus Nielsen
 *
 */
public class EventReplayer implements Runnable {

    private DocumentEventCapturer dec;
    private JTextArea area;

    private Socket connection;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

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
        System.out.println("I'm the thread running the ddist.EventReplayer, now I die!");
    }

    private void sendEvent(MyTextEvent mte) {
        //TODO
    }

    public void newConnection(Socket socket){
        connection = socket;
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
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
