package ddist.events;


public class InitialSetupEvent implements Event{
    public InitialSetupEvent(String areaText, double time) {

    }

    @Override
    public double getTimestamp() {
        return 0;
    }

    @Override
    public void setTimestamp(double timestamp) {

    }
}
