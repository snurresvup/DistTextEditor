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
            // Reset upper textfield and make it uneditable
            dte.resetText();
            dte.getArea1().setEditable(false);
            // Set the title and start listening
            dte.setTitle("I'm listening on " + dte.getLocalHostAddress() +
                    ":" + dte.getPortNumber());
            dte.setSocket(dte.getServer().accept());
            // Upon connection we set the connected flag and change the title. Then we interupt the event replayer so
            // that it notices the new state, and changes its behaviour. The text field is now editable again.
            dte.setConnected(true);
            dte.setTitle("Connected to " +
                    dte.getSocket().getRemoteSocketAddress().toString());
            dte.interruptEventReplayer();
            dte.getArea1().setEditable(true);
        } catch (IOException ioe) {
            // Do nothing
        }
    }
}
