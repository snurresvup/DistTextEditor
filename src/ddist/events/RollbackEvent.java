package ddist.events;


import ddist.events.text.TextEvent;

import java.util.ArrayList;

public class RollbackEvent implements Event {
    private ArrayList<TextEvent> events;

    public RollbackEvent(ArrayList<TextEvent> events) {

        this.events = events;
    }

    public ArrayList<TextEvent> getEvents() {
        return events;
    }
}
