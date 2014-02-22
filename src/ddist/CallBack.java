package ddist;


public interface CallBack {
    public double getTime();
    public void setTime(double newTime);
    public void incTime();
    public void setTitleOfWindow(String titleOfWindow);
    public void setID(double id);
    public double getID();
    public void setConnect(boolean enabled);
    public void setDisconnect(boolean enabled);
    void setListen(boolean enabled);
}
