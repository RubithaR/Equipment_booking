package com.smartlab.bookingservice.notifier;

public interface Notifier {
    void publish(NotificationEvent event);
}
