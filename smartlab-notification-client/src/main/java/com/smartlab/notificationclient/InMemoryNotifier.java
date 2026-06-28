package com.smartlab.notificationclient;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Test double — captures published events in order for assertions. */
public class InMemoryNotifier<E> implements Notifier<E> {

    private final List<E> published = new CopyOnWriteArrayList<>();

    @Override
    public void publish(E event) {
        published.add(event);
    }

    public List<E> published() {
        return List.copyOf(published);
    }

    public void clear() {
        published.clear();
    }
}
