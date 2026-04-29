package com.smartlab.userservice.notifier;

public interface Notifier {
    void publish(NotificationEvent event);
}
