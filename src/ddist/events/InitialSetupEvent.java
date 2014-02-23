package ddist.events;


public class InitialSetupEvent implements Event {
    private String areaText;
    private double time4Client;
    private double timestamp;

    public InitialSetupEvent(String areaText, double time4Client, double timestamp) {
        this.areaText = areaText;

        this.time4Client = time4Client;
        this.timestamp = timestamp;
    }

    public String getAreaText() {
        return areaText;
    }

    public double getTime4Client() {
        return time4Client;
    }
    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }
}
