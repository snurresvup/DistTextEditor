import java.io.ObjectInputStream;
import java.net.Socket;

public class ServerThread implements Runnable {

    Socket socket;
    DocumentEventCapturer dec;

    public ServerThread(Socket socket, DocumentEventCapturer dec){
        this.socket = socket;
        this.dec = dec;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            MyTextEvent textEvent = (MyTextEvent) objectInputStream.readObject();
            dec.addTextEvent(textEvent);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
