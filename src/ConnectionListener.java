import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;

public class ConnectionListener implements Runnable {
    private DistributedTextEditor dte;

    public ConnectionListener(DistributedTextEditor distributedTextEditor) {
        this.dte = distributedTextEditor;
    }

    @Override
    public void run() {
        try {
            dte.resetText();
            dte.getArea1().setEditable(false);
            dte.setTitle("I'm listening on " + dte.getLocalHostAddress() +
                    ":" + dte.getPortNumber());
            dte.setSocket(dte.getServer().accept());
            dte.setConnected(true);
            dte.setTitle("Connected to " +
                    dte.getSocket().getRemoteSocketAddress().toString());
            dte.interruptEventReplayer();
            dte.getArea1().setEditable(true);
        } catch (IOException ioe) {
            System.out.println("Server stopped listening");
        }
    }
}
