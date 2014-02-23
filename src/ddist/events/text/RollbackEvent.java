package ddist.events.text;

import ddist.events.text.TextEvent;

import java.util.SortedMap;

public class RollbackEvent extends TextEvent{

    private SortedMap<Double, TextEvent> rollbackMap;

    public RollbackEvent(int offset, SortedMap<Double, TextEvent> rollbackMap){
        super(offset);
        this.rollbackMap = rollbackMap;
    }

    public SortedMap<Double, TextEvent> getRollbackMap() {
        return rollbackMap;
    }
}
