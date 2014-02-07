import java.io.IOException;
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
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(true){ // TODO should run until dced
            System.out.println("This is the server thread reading.");
            try {
                MyTextEvent textEvent = (MyTextEvent) objectInputStream.readObject();
                dec.addTextEvent(textEvent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
