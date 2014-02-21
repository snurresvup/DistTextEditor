package ddist.events;

import java.io.Serializable;

public interface Event extends Serializable{
    public double getTimestamp();
    public void setTimestamp(double timestamp);
}
