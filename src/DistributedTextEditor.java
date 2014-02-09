
import com.sun.deploy.uitoolkit.UIToolkit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import java.net.*;

public class DistributedTextEditor extends JFrame {

    private JTextArea area1 = new JTextArea(20,120);
    private JTextArea area2 = new JTextArea(20,120);
    private JTextField ipaddress = new JTextField("IP address here");
    private JTextField portNumber = new JTextField("Port number here");

    private EventReplayer er;
    private Thread ert;

    private JFileChooser dialog =
            new JFileChooser(System.getProperty("user.dir"));

    private String currentFile = "Untitled";
    private boolean changed = false;
    private boolean connected = false;
    private DocumentEventCapturer dec = new DocumentEventCapturer();

    private ServerSocket server;
    private Socket socket = null;

    public DistributedTextEditor() {
        area1.setFont(new Font("Monospaced",Font.PLAIN,12));

        area2.setFont(new Font("Monospaced",Font.PLAIN,12));
        ((AbstractDocument)area1.getDocument()).setDocumentFilter(dec);
        area2.setEditable(false);

        Container content = getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JScrollPane scroll1 =
                new JScrollPane(area1,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        content.add(scroll1,BorderLayout.CENTER);

        JScrollPane scroll2 =
                new JScrollPane(area2,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        content.add(scroll2,BorderLayout.CENTER);

        content.add(ipaddress,BorderLayout.CENTER);
        content.add(portNumber,BorderLayout.CENTER);

        JMenuBar JMB = new JMenuBar();
        setJMenuBar(JMB);
        JMenu file = new JMenu("File");
        JMenu edit = new JMenu("Edit");
        JMB.add(file);
        JMB.add(edit);

        file.add(Listen);
        file.add(Connect);
        file.add(Disconnect);
        file.addSeparator();
        file.add(Save);
        file.add(SaveAs);
        file.add(Quit);

        edit.add(Copy);
        edit.add(Paste);
        edit.getItem(0).setText("Copy");
        edit.getItem(1).setText("Paste");

        Save.setEnabled(false);
        SaveAs.setEnabled(false);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        area1.addKeyListener(k1);
        setTitle("Disconnected");
        setVisible(true);
        area1.insert("Example of how to capture stuff from the event queue and replay it in another buffer.\n" +
                "Try to type and delete stuff in the top area.\n" +
                "Then figure out how it works.\n", 0);

        er = new EventReplayer(dec, area2, this);
        ert = new Thread(er);
        ert.start();
    }

    private KeyListener k1 = new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
            changed = true;
            Save.setEnabled(true);
            SaveAs.setEnabled(true);
        }
    };

    Action Listen = new AbstractAction("Listen") {
        public void actionPerformed(ActionEvent e) {
            saveOld();

            // Creating a serversocket
            startServer();
            // Starting a thread listening for an incoming connection. Once the connection is established the socket is
            // saved in a field, and then the thread terminates.
            startConnectionListener();

            changed = false;
            Save.setEnabled(false);
            SaveAs.setEnabled(false);
        }
    };

    Action Connect = new AbstractAction("Connect") {
        public void actionPerformed(ActionEvent e) {
            saveOld();

            // Trying to connect to the specified server and port
            connectToServer();

            changed = false;
            Save.setEnabled(false);
            SaveAs.setEnabled(false);
        }
    };

    Action Disconnect = new AbstractAction("Disconnect") {
        public void actionPerformed(ActionEvent e) {
            setTitle("Disconnected");
            // We only do anything when we are connected.
            if (connected) {
                connected = false;
                // Notifies the event replayer that we are not connected anymore.
                interruptEventReplayer();
                area1.setText("");
                try {
                    // Closing the connected socket
                    socket.close();
                } catch (IOException ioe) {
                    // Shhh tell nobody
                }
            }
            // In the case where we act as a server. We want to close the server socket and, thereby stop listening
            if (getServer() != null) {
                stopServer();
            }
        }
    };

    Action Save = new AbstractAction("Save") {
        public void actionPerformed(ActionEvent e) {
            if(!currentFile.equals("Untitled"))
                saveFile(currentFile);
            else
                saveFileAs();
        }
    };

    Action SaveAs = new AbstractAction("Save as...") {
        public void actionPerformed(ActionEvent e) {
            saveFileAs();
        }
    };

    Action Quit = new AbstractAction("Quit") {
        public void actionPerformed(ActionEvent e) {
            saveOld();
            System.exit(0);
        }
    };

    ActionMap m = area1.getActionMap();

    Action Copy = m.get(DefaultEditorKit.copyAction);
    Action Paste = m.get(DefaultEditorKit.pasteAction);

    private void saveFileAs() {
        if(dialog.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
            saveFile(dialog.getSelectedFile().getAbsolutePath());
    }

    private void saveOld() {
        if(changed) {
            if(JOptionPane.showConfirmDialog(this, "Would you like to save "+ currentFile +" ?","Save",JOptionPane.YES_NO_OPTION)== JOptionPane.YES_OPTION)
                saveFile(currentFile);
        }
    }

    private void saveFile(String fileName) {
        try {
            FileWriter w = new FileWriter(fileName);
            area1.write(w);
            w.close();
            currentFile = fileName;
            changed = false;
            Save.setEnabled(false);
        }
        catch(IOException e) {
        }
    }

    public static void main(String[] arg) {
        new DistributedTextEditor();
    }

    private void startServer() {
        try {
            // If there is an open server socket, we close it.
            if (server != null && server.isBound()) {
                server.close();
            }
            // Creates the server socket
            setServer(new ServerSocket(getPortNumber()));
        } catch (IOException e) {
            setTitle("There is already something bound on the port " + getPortNumber());
        }
    }

    public void startConnectionListener() {
        // We start the server listener passing the distributed text editor. In that way the thread can change its
        // state upon connection and so forth.
        Thread listener = new Thread(new ConnectionListener(this));
        listener.start();
    }

    public void resetText() {
        area1.setText("");
    }

    private void stopServer() {
        if (server != null && server.isBound()) {
            try {
                server.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        area1.setEditable(true);
        server = null;
    }

    public String getLocalHostAddress() {
        String address = null;
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            address = localhost.getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("Cannot resolve the Internet address of the local host.");
            System.err.println(e);
            System.exit(-1);
        }
        return address;
    }

    public int getPortNumber() {
        int port;
        try {
            port = Integer.parseInt(portNumber.getText());
            if (port < 0 || port > 65536) {
                port = 1337; // We default to this portnumber
            }
        } catch (NumberFormatException _) {
            port = 1337; // if there is text in the port field
        }
        return port;
    }

    private void connectToServer() {
        String ip = getAddress();
        int port = getPortNumber();
        try {
            setTitle("Connecting to " + ip + ":" + port + "...");
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 3000);
            resetText();
            setTitle("Connected to " + ip + ":" + port);
            connected = true;
            interruptEventReplayer();
        } catch (UnknownHostException e) {
            setTitle("Error connecting to " + ip + ":" + port + "! Maybe you got the wrong ip address?!");
            e.printStackTrace();
        } catch (IOException e) {
            setTitle("Connection refused");
        }
    }

    private String getAddress() {
        String address = ipaddress.getText();
        return address;
    }

    public ServerSocket getServer() {
        return server;
    }

    public void setServer(ServerSocket serverSocket) {
        this.server = serverSocket;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void interruptEventReplayer() {
        ert.interrupt();
    }

    public boolean isConnected() {
        return connected;
    }

    public JTextArea getArea1() {
        return area1;
    }

    public JTextArea getArea2() {
        return area2;
    }
}
