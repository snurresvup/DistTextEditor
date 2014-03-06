package ddist;

import ddist.events.text.TextEvent;
import ddist.events.text.TextInsertEvent;
import ddist.events.text.TextRemoveEvent;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 *
 * This class captures and remembers the text events of the given document on
 * which it is put as a filter. Normally a filter is used to put restrictions
 * on what can be written in a buffer. In out case we just use it to see all
 * the events and make a copy. 
 *
 * @author Jesper Buus Nielsen
 *
 */
public class DocumentEventCapturer extends DocumentFilter {

    private CallBack callBack;

    /*
     * We are using a blocking queue for two reasons: 
     * 1) They are thread safe, i.e., we can have two threads add and take elements 
     *    at the same time without any race conditions, so we do not have to do  
     *    explicit synchronization.
     * 2) It gives us a member take() which is blocking, i.e., if the queue is
     *    empty, then take() will wait until new elements arrive, which is what
     *    we want, as we then don't need to keep asking until there are new elements.
     */
    protected LinkedBlockingQueue<TextEvent> eventHistory = new LinkedBlockingQueue<>();
    private volatile boolean filtering = false;
    private ArrayList<TextEvent> unExecutedTextEvents = new ArrayList<>();

    public DocumentEventCapturer(CallBack callBack) {
        this.callBack = callBack;
    }

    /**
     * If the queue is empty, then the call will block until an element arrives.
     * If the thread gets interrupted while waiting, we throw InterruptedException.
     *
     * @return Head of the recorded event queue. 
     */
    TextEvent take() throws InterruptedException {
        return eventHistory.take();
    }

    public void insertString(FilterBypass fb, int offset,
                             String str, AttributeSet a)
            throws BadLocationException {
	
	/* Queue a copy of the event and then modify the textarea */
        if(filtering){
            TextInsertEvent insertEvent = new TextInsertEvent(offset, str, callBack.getTimestamp());
            updateOffset(insertEvent);
            addToEventHistory(insertEvent);
        }else{
            insertString4Realz(fb,offset,str,a);
        }
    }

    /*
     * Updates the offset of TextRemoveEvents and TextInsertEvents, and the length of TextRemoveEvents.
     * According to un-executed local events.
     * @param event
     * The event to be updated.
     */
    private void updateOffset(TextEvent event) {
        if(event instanceof TextInsertEvent) {
            updateOffsetOfInsertEvent((TextInsertEvent)event);
        } else if (event instanceof TextRemoveEvent){
            updateOffsetOfRemoveEvent((TextRemoveEvent)event);
        }
    }

    private void updateOffsetOfRemoveEvent(TextRemoveEvent event) {
        /*
         * For every unexecuted local remove event the new event's offset and length has to be updated
         * according to the following cases.
         * e = unexecuted event.
         */
        for(TextEvent e: unExecutedTextEvents) {
            /*
             * If the offset of e is to the right of the text we are to remove, do nothing
             */
            if(e.getOffset() > event.getOffset() + event.getLength()) {
                break;
            }
            /*
             * If e is a TextInsertEvent and e's offset is to the left of the new event, add the length of
             * the text inserted by e to the new events offset.
             */
            else if (e instanceof TextInsertEvent && e.getOffset() < event.getOffset()) {
                event.setOffset(event.getOffset() + ((TextInsertEvent) e).getText().length());
            }
            /*
             * If e is a TextInsertEvent to be inserted inside the text the new event is to remove, increase
             * the lenght of the new event by the length of e's text. (remove e's text as well)
             */
            else if (e instanceof TextInsertEvent &&
                    e.getOffset() >= event.getOffset() &&
                    e.getOffset() < event.getOffset() + event.getLength()) {
                event.setLength(event.getLength() + ((TextInsertEvent) e).getText().length());
            }
            /*
             * If e is a TextRemoveEvent and e is to remove some text to the left of what the new event is removing,
             * decrease the offset of the new event by the length of what e is removing.
             */
            else if (e instanceof TextRemoveEvent &&
                    e.getOffset() + ((TextRemoveEvent)e).getLength() < event.getOffset()) {
                event.setOffset(event.getOffset() - ((TextRemoveEvent) e).getLength());
            }
            /*
             * If e is a TextRemoveEvent and e is removing the same or more text than the new event set the length
             * of the new remove event to 0.
             */
            else if (e instanceof TextRemoveEvent &&
                    e.getOffset() <= event.getOffset() &&
                    e.getOffset() + ((TextRemoveEvent) e).getLength() >= event.getOffset() + event.getLength()) {
                event.setLength(0);
            }
            /*
             * If e is a TextRemoveEvent and e overlapping some of the new event from the right, decrease the length
             * of the new event by the length of the overlap.
             */
            else if (e instanceof TextRemoveEvent &&
                    e.getOffset() > event.getOffset() &&
                    e.getOffset() + ((TextRemoveEvent) e).getLength() > event.getOffset() + event.getLength()) {
                event.setLength(event.getLength() - (event.getOffset() + event.getLength() - e.getOffset()));
            }
            /*
             * If e is a TextRemoveEvent and e is removing some of the same text as the new event, decrease the length
             * of the new event by the length of e.
             */
            else if (e instanceof TextRemoveEvent &&
                    e.getOffset() > event.getOffset() &&
                    e.getOffset() + ((TextRemoveEvent) e).getLength() <= event.getOffset() + event.getLength()) {
                event.setLength(event.getLength() - ((TextRemoveEvent) e).getLength());
            }
            /*
             * If e is a TextRemoveEvent, and e is overlapping the new event from the left, decrease the length of the
             * new event by the length of the overlap, and increase the offset of the new event such that the new event
             * begins where e ends.
             */
            else if (e instanceof TextRemoveEvent &&
                    e.getOffset() <= event.getOffset() &&
                    e.getOffset() + ((TextRemoveEvent) e).getLength() < event.getOffset() + event.getLength()) {
                event.setLength(event.getOffset() + event.getLength() - (e.getOffset() + ((TextRemoveEvent) e).getLength()));
                event.setOffset(e.getOffset() + ((TextRemoveEvent) e).getLength());
            }
        }
    }

    private void updateOffsetOfInsertEvent(TextInsertEvent event) {
        /*
         * For every unexecuted local insert event the new event's offset has to be updated
         * according to the following cases.
         * e = unexecuted event.
         */
        for(TextEvent e: unExecutedTextEvents) {
            /*
             * If e's offset is larger than the offset of the new event, do nothing.
             */
            if(e.getOffset() > event.getOffset()) {
                break;
            }
            /*
             * If e is a TextInsertEvent with the same or smaller offset than the new event, increase the offset of
             * the new event by the length of e's text.
             */
            else if (e instanceof TextInsertEvent) {
                event.setOffset(event.getOffset() + ((TextInsertEvent) e).getText().length());
            }
            /*
             * If e is a TextRemoveEvent and the new event is to the right of the end of e, decrease the offset of
             * the new event by the length of the text removed by e.
             */
            else if (e instanceof TextRemoveEvent && event.getOffset() >= e.getOffset()+((TextRemoveEvent) e).getLength()) {
                event.setOffset(event.getOffset() - ((TextRemoveEvent) e).getLength());
            }
            /*
             * If e is a TextRemoveEvent and the new event is writing inside some text to be removed by e, decrease the
             * offset of the new event by the difference of the offsets of e and the new event.
             */
            else if (e instanceof TextRemoveEvent && event.getOffset() < e.getOffset()+((TextRemoveEvent) e).getLength()) {
                event.setOffset(event.getOffset() - (event.getOffset() - e.getOffset()));
            }
        }
    }

    private void addToEventHistory(TextEvent event) {
        unExecutedTextEvents.add(event);
        eventHistory.add(event);
    }

    public void remove(FilterBypass fb, int offset, int length)
            throws BadLocationException {
	/* Queue a copy of the event and then modify the textarea */
        if(filtering){
            TextRemoveEvent removeEvent = new TextRemoveEvent(offset, length, callBack.getTimestamp());
            updateOffset(removeEvent);
            addToEventHistory(removeEvent);
        }else{
            remove4Realz(fb,offset,length);
        }
    }

    public void replace(FilterBypass fb, int offset,
                        int length,
                        String str, AttributeSet a)
            throws BadLocationException {
	
	/* Queue a copy of the event and then modify the text */
        System.out.println(unExecutedTextEvents);
        if(filtering){
            if (length > 0) {
                TextRemoveEvent removeEvent = new TextRemoveEvent(offset, length, callBack.getTimestamp());
                updateOffset(removeEvent);
                addToEventHistory(removeEvent);
            }
            TextInsertEvent insertEvent = new TextInsertEvent(offset, str, callBack.getTimestamp());
            updateOffset(insertEvent);
            addToEventHistory(insertEvent);
        }else{
            replace4Realz(fb,offset,length,str,a);
        }
    }

    public void insertString4Realz(FilterBypass fb, int offset, String str, AttributeSet a) throws BadLocationException{
        super.insertString(fb, offset, str, a);
    }
      
    public void remove4Realz(FilterBypass fb, int offset, int length) throws BadLocationException {
        super.remove(fb, offset, length);
    }

    public void replace4Realz(FilterBypass fb, int offset, int length, String str, AttributeSet a)
            throws BadLocationException {
        super.replace(fb, offset, length, str, a);
    }

    public synchronized void setFilter(boolean enabled){
        filtering = enabled;
    }

    public void markEventAsExecuted(TextEvent textEvent) {
        unExecutedTextEvents.remove(textEvent);
    }
}
