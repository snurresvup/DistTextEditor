package ddist;

import ddist.events.text.MyTextEvent;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static java.lang.Thread.interrupted;

public class EventSender implements Runnable{

    private DocumentEventCapturer dec;
    private Socket socket;
    private ObjectOutputStream outputStream;

    public EventSender(DocumentEventCapturer dec) {
        this.dec = dec;
    }

    public void newConnection(Socket socket) {
        this.socket = socket;
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (!interrupted()) {
            try {
                MyTextEvent mte = dec.take();
                sendEvent(mte);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendEvent(MyTextEvent mte) {
        try {
            outputStream.writeObject(mte);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
