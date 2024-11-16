package engine.event;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    private final Map<Class<? extends Event>, List<EventListener<?>>> listeners;
    private final Queue<Event> eventQueue;
    private boolean isProcessing;

    public EventBus() {
        this.listeners = new ConcurrentHashMap<>();
        this.eventQueue = new LinkedList<>();
    }

    /**
     * Subscribe to events of a specific type
     */
    public <T extends Event> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Unsubscribe from events of a specific type
     */
    public <T extends Event> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
        List<EventListener<?>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
            if (eventListeners.isEmpty()) {
                listeners.remove(eventType);
            }
        }
    }

    /**
     * Emit an event immediately if not processing events, otherwise queue it
     */
    public void emit(Event event) {
        if (isProcessing) {
            eventQueue.offer(event);
        } else {
            processEvent(event);
        }
    }

    /**
     * Process all queued events
     */
    public void update() {
        isProcessing = true;

        Event event;
        while ((event = eventQueue.poll()) != null) {
            processEvent(event);
        }

        isProcessing = false;
    }

    @SuppressWarnings("unchecked")
    private void processEvent(Event event) {
        List<EventListener<?>> eventListeners = listeners.get(event.getClass());

        if (eventListeners != null) {
            for (EventListener<?> listener : eventListeners) {
                if (event.isHandled()) break;

                ((EventListener<Event>) listener).onEvent(event);
            }
        }
    }
}
