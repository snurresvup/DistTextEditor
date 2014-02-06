import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ServerThread implements Runnable {

    Socket socket;
    DocumentEventCapturer dec;

    public ServerThread(Socket socket, DocumentEventCapturer dec){
        this.socket = socket;
        this.dec = dec;
    }

    @Override
    public void run() {
        while(true){
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                ArrayList<MyTextEvent> textEvent = (ArrayList<MyTextEvent>) objectInputStream.readObject();
                for(MyTextEvent t : textEvent){
                    dec.addTextEvent(t);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
