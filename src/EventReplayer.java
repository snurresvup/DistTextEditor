import javax.swing.JTextArea;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
    protected Socket socket;

    public EventReplayer(DocumentEventCapturer dec, JTextArea area, Socket socket) {
        this.dec = dec;
        this.area = area;
        this.socket = socket;
    }

    public void run() {
        startListeningThread();

        boolean wasInterrupted = false;
        ObjectOutputStream outputStream = null;
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (!wasInterrupted) {
            try {
                MyTextEvent myTextEvent = dec.take();
                outputStream.writeObject(myTextEvent);
                outputStream.flush();
            } catch (IOException|InterruptedException e) {
                e.printStackTrace();
                wasInterrupted = true;
            }
        }
        System.out.println("I'm the thread running the EventReplayer, now I die!");
    }

    private void startListeningThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    while (true){//TODO
                        MyTextEvent mte = (MyTextEvent)inputStream.readObject();
                        if (mte instanceof TextInsertEvent) {
                            final TextInsertEvent tie = (TextInsertEvent)mte;
                            EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    try {
                                        area.insert(tie.getText(), tie.getOffset());
                                    } catch (Exception e) {
                                        System.err.println(e);
				                                /* We catch all exceptions, as an uncaught exception would make the
				                                * EDT unwind, which is not healthy.
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
                                            /* We catch all exceptions, as an uncaught exception would make the
                                             * EDT unwind, which is not healthy.
                                             */
                                    }
                                }
                            });
                        }
                    }
                } catch (IOException |ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
