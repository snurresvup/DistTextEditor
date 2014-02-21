package ddist.events;

public class ClearTextEvent implements Event {
    private double time;

    public ClearTextEvent(double time) {


        this.time = time;
    }

    @Override
    public double getTimestamp() {
        return time;
    }

    @Override
    public void setTimestamp(double timestamp) {
        this.time = timestamp;
    }
}
