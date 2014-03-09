package ddist;

public interface CallBack {
    public VectorClock getTimestamp();
    public void setTime(VectorClock newTime);
    public void setTitleOfWindow(String titleOfWindow);
    public void setID(int id);
    public int getID();
    public String getIp();
    public Integer getPort();
    public void setConnect(boolean enabled);
    public void setDisconnect(boolean enabled);
    public void setListen(boolean enabled);
    public void startListeningThread(int port);

    public VectorClock getTime();
}
