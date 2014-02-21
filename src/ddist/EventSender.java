package ddist;

import ddist.events.text.MyTextEvent;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.interrupted;

public class EventSender implements Runnable{

    private DocumentEventCapturer dec;
    private Socket socket;
    private ObjectOutputStream outputStream;
    private LinkedBlockingQueue queue;

    public EventSender(DocumentEventCapturer dec) {
        this.dec = dec;
        socket = null;
        queue = new LinkedBlockingQueue();
        receiveLocalEvents();
    }

    private void receiveLocalEvents() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!interrupted()) {
                    try {
                        queue.put(dec.take());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
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
                Object event = queue.take();
                sendEvent(event);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendEvent(Object event) {
        try {
            outputStream.writeObject(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
