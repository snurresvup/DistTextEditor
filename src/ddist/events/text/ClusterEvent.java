package ddist.events.text;

import java.util.SortedMap;


public class ClusterEvent extends TextEvent{

    private SortedMap<Double, TextEvent> toPlay;

    public ClusterEvent(int offset, SortedMap<Double, TextEvent> toPlay) {
        super(offset);
        this.toPlay = toPlay;
    }

    public SortedMap<Double, TextEvent> getToPlay() {
        return toPlay;
    }
}
