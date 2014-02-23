package ddist;

import ddist.events.ConnectionEvent;
import ddist.events.DisconnectEvent;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DistributedTextEditor extends JFrame implements CallBack {

    public JTextArea getArea() {
        return area;
    }

    private JTextArea area = new JTextArea(40,120);
    private JTextField ipaddress = new JTextField("IP address here");
    private JTextField portNumber = new JTextField("Port number here");
    protected ServerSocket serverSocket;

    private EventManager em;
    private Thread emt;

    private JFileChooser dialog =
            new JFileChooser(System.getProperty("user.dir"));

    private String currentFile = "Untitled";
    private boolean changed = false;
    private Double time = 0.0;
    private DocumentEventCapturer dec = new DocumentEventCapturer(this);
    private double id;
    private boolean server = false;

    public DistributedTextEditor() {
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));

        ((AbstractDocument) area.getDocument()).setDocumentFilter(dec);

        Container content = getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JScrollPane scroll1 =
                new JScrollPane(area,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        content.add(scroll1,BorderLayout.CENTER);

        content.add(ipaddress,BorderLayout.CENTER);
        content.add(portNumber,BorderLayout.CENTER);

        JMenuBar JMB = new JMenuBar();
        setJMenuBar(JMB);
        JMenu file = new JMenu("File");
        JMenu con = new JMenu("Connection");
        JMB.add(file);
        JMB.add(con);

        con.add(Listen);
        con.add(StopListening);
        con.addSeparator();
        con.add(Connect);
        con.add(Disconnect);
        file.add(Save);
        file.add(SaveAs);
        file.add(Quit);

        Disconnect.setEnabled(false);
        StopListening.setEnabled(false);
        Save.setEnabled(false);
        SaveAs.setEnabled(false);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        area.addKeyListener(k1);
        setTitle("Disconnected");
        setVisible(true);
        area.insert("Example of how to capture stuff from the event queue and replay it in another buffer.\n" +
                "Try to type and delete stuff in the top area.\n" +
                "Then figure out how it works.\n", 0);

        em = new EventManager(area, dec, this);
        emt = new Thread(em);
        emt.start();
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
            startListeningThread();
        }
    };

    Action StopListening = new AbstractAction("Stop Listening") {
        @Override
        public void actionPerformed(ActionEvent e) {
            server = false;
            setTitle("Disconnected");
            Listen.setEnabled(true);
            StopListening.setEnabled(false);
        }
    };

    private String getHostAddress() throws UnknownHostException {
        String host;
        host = InetAddress.getLocalHost().getHostAddress();
        return host;
    }

    public void startListeningThread() {
        Thread listeningThread = new Thread(new Runnable() {
            @Override
            public void run() {
                server = true;
                try {
                    setTitle("I'm listening on " + getHostAddress() + ":"+ getPortNumber());
                    serverSocket = new ServerSocket(getPortNumber());
                    Socket socket = serverSocket.accept();
                    em.queueEvent(new ConnectionEvent(socket));
                    serverSocket.close();
                    serverSocket = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Listen.setEnabled(false);
                StopListening.setEnabled(true);
                Connect.setEnabled(false);
                changed = false;
                Save.setEnabled(false);
                SaveAs.setEnabled(false);
            }
        });
        listeningThread.start();
    }

    Action Connect = new AbstractAction("Connect") {
        public void actionPerformed(ActionEvent e) {
            saveOld();
            setTitle("Connecting to " + ipaddress.getText() + ":" + portNumber.getText() + "...");
            startConnectionThread();
            changed = false;
            Save.setEnabled(false);
            SaveAs.setEnabled(false);
        }
    };

    public void setConnect(boolean enabled) {
        Connect.setEnabled(enabled);
    }

    public void setDisconnect(boolean enabled) {
        Disconnect.setEnabled(enabled);
    }

    public void setListen(boolean enabled) {
        Listen.setEnabled(enabled);
    }

    public void setStopListening(boolean enabled) {
        StopListening.setEnabled(enabled);
    }

    private void startConnectionThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(getIpField(), getPortNumber());
                    em.queueEvent(new ConnectionEvent(socket));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                }
        }).start();
    }

    Action Disconnect = new AbstractAction("Disconnect") {
        public void actionPerformed(ActionEvent e) {

            setTitle("Disconnected");

            em.disconnected();

            Disconnect.setEnabled(false);
            if (!StopListening.isEnabled()) {
                Listen.setEnabled(true);
            }
        }
    };

    @Override
    public boolean isServer() {
        return server;
    }

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

    private void saveFileAs() {
        if(dialog.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
            saveFile(dialog.getSelectedFile().getAbsolutePath());
    }

    private void saveOld() {
        if(changed) {
            if(JOptionPane.showConfirmDialog(this, "Would you like to save "+ currentFile +" ?",
                    "Save",JOptionPane.YES_NO_OPTION)== JOptionPane.YES_OPTION)
                saveFile(currentFile);
        }
    }

    private void saveFile(String fileName) {
        try {
            FileWriter w = new FileWriter(fileName);
            area.write(w);
            w.close();
            currentFile = fileName;
            changed = false;
            Save.setEnabled(false);
        }
        catch(IOException e) {
        }
    }

    private int getPortNumber() {
        int res;

        Pattern pattern = Pattern.compile(
                "0*([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-6])");
        Matcher matcher = pattern.matcher(portNumber.getText());

        if(matcher.matches()){
            try{
                res = Integer.parseInt(portNumber.getText());
            }catch (NumberFormatException e){
                res = 1337;
            }
        }else{
            res = 1337;
        }
        return res;
    }

    private String getIpField(){
        String res;
        Pattern IPv4 = Pattern.compile("^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");
        Matcher matcher = IPv4.matcher(ipaddress.getText());

        if(matcher.matches()){
            res = ipaddress.getText();
        }else {
            res = "localhost";
        }

        return res;
    }

    public void incTime(){
        synchronized (time){
            time++;
        }
    }

    @Override
    public void setTitleOfWindow(String titleOfWindow) {
        setTitle(titleOfWindow);
    }

    @Override
    public void setID(double id) {
        this.id = id;
    }

    @Override
    public double getID() {
        return id;
    }

    public void setTime(double newTime){
        synchronized (time){
            time = newTime;
        }
    }

    public double getTime(){
        synchronized (time){
            return time;
        }
    }

    public static void main(String[] arg) {
        new DistributedTextEditor();
    }

}
