package ddist;

public interface CallBack {
    public double getTimestamp();
    public void setTime(double newTime);
    public void setTitleOfWindow(String titleOfWindow);
    public void setID(double id);
    public double getID();
    public String getIp();
    public Integer getPort();
    public void setConnect(boolean enabled);
    public void setDisconnect(boolean enabled);
    public void setListen(boolean enabled);
    public void setStopListening(boolean b);
    public void startListeningThread(int port);

    public double getTime();
}
