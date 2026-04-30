package com.smartlab.notificationclient;

/**
 * Port for emitting notification events. Each service supplies its own
 * domain-specific event type {@code E} (typically a sealed interface) and an
 * adapter that translates events to dispatch requests.
 */
public interface Notifier<E> {
    void publish(E event);
}
