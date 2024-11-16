package engine.event;

/**
 * Base class for all events in the system.
 * Events are immutable data carriers.
 */
public abstract class Event {
    private boolean handled = false;

    /**
     * Mark event as handled to prevent further processing
     */
    public void handle() {
        handled = true;
    }

    public boolean isHandled() {
        return handled;
    }
}
