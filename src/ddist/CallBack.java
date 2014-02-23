package ddist;


import ddist.events.text.TextEvent;

import javax.swing.*;
import java.util.SortedMap;

public interface CallBack {
    public double getTime();
    public void setTime(double newTime);
    public void incTime();
    public void setTitleOfWindow(String titleOfWindow);
    public void setID(double id);
    public double getID();
    public void setConnect(boolean enabled);
    public void setDisconnect(boolean enabled);
    public void setListen(boolean enabled);
    public void setStopListening(boolean b);
    public boolean isServer();
    public void startListeningThread();
    public JTextArea getArea();
    public SortedMap<Double,TextEvent> getLog();
}
