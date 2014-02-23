package ddist.events;


public class InitialSetupEvent implements Event {
    private String areaText;
    private double clientOffset;
    private double timestamp;

    public InitialSetupEvent(String areaText, double clientOffset, double timestamp) {
        this.areaText = areaText;

        this.clientOffset = clientOffset;
        this.timestamp = timestamp;
    }

    public String getAreaText() {
        return areaText;
    }

    public double getClientOffset() {
        return clientOffset;
    }
    public double getTimestamp() {
        return timestamp;
    }
}
