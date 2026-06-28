package com.smartlab.userservice.notifier;

import com.smartlab.notificationclient.Notifier;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryNotifier implements Notifier<NotificationEvent> {

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
