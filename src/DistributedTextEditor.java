
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

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
    protected boolean listening = false;

    protected ServerSocket serverSocket;


    private DocumentEventCapturer dec = new DocumentEventCapturer();
    private Socket socket;

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
            area1.setText("");
            /*
            * - Accept a connection
            * - Create a thread to deal with each of the clients
            * */
            int port;
            try {
                port = Integer.parseInt(portNumber.getText());
                if (port < 0 || port > 65536) {
                    port = 1337; // We default to this portnumber
                }
            } catch (NumberFormatException _) {
                port = 1337;
            }

            String localhost = null;

            try {
                InetAddress inetAddress = InetAddress.getLocalHost();
                localhost = inetAddress.getHostAddress();
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            }

            try {
                serverSocket = new ServerSocket(port);
                setTitle("I'm listening on " + localhost + ":"+port);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            changed = false;
            listening = true;
            Save.setEnabled(false);
            SaveAs.setEnabled(false);


            while(listening){
                new Thread(){
                    public void run(){
                        Socket res;
                        try {
                            res = serverSocket.accept();
                            startCommunicationThread(res);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }
    };

    protected void startCommunicationThread(Socket socket){
        er = new EventReplayer(dec, area2, socket);
        ert = new Thread(er);
        ert.start();
    }


    Action Connect = new AbstractAction("Connect") {
        public void actionPerformed(ActionEvent e) {
            saveOld();
            area1.setText("");
            setTitle("Connecting to " + ipaddress.getText() + ":" + portNumber.getText() + "...");

            try {
                socket = new Socket(ipaddress.getText(), Integer.parseInt(portNumber.getText()));

                if(socket.isConnected()){
                    connected = true;
                    setTitle("Connected to " + ipaddress.getText() + ":" + portNumber.getText() + "!");
                    startCommunicationThread(socket);
                }else{
                    setTitle("Failed to connect to" + ipaddress.getText() + ":" + portNumber.getText() + "!");
                }

            } catch (IOException e1) {
                e1.printStackTrace();
            }

            changed = false;
            Save.setEnabled(false);
            SaveAs.setEnabled(false);
        }
    };

    Action Disconnect = new AbstractAction("Disconnect") {
        public void actionPerformed(ActionEvent e) {
            setTitle("Disconnected");
            // TODO should destroy all server-client threads, serversockets and sockets.
            if(listening){
                try {
                    serverSocket.close();
                    listening = false;
                    ert.interrupt();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }else if(connected){
                try {
                    socket.close();
                    socket = null;
                    ert.interrupt();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
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

}
