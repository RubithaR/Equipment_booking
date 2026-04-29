package com.smartlab.userservice.notifier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryNotifier implements Notifier {

    private final List<NotificationEvent> published = new CopyOnWriteArrayList<>();

    @Override
    public void publish(NotificationEvent event) {
        published.add(event);
    }

    public List<NotificationEvent> published() {
        return List.copyOf(published);
    }

    public void clear() {
        published.clear();
    }
}
