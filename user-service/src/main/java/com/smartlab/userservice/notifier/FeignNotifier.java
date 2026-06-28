package com.smartlab.userservice.notifier;

import com.smartlab.notificationclient.NotificationClient;
import com.smartlab.notificationclient.NotificationDispatchRequest;
import com.smartlab.notificationclient.Notifier;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FeignNotifier implements Notifier<NotificationEvent> {

    private static final Logger log = LoggerFactory.getLogger(FeignNotifier.class);

    private final NotificationClient notificationClient;

    @Override
    public void publish(NotificationEvent event) {
        switch (event) {
            case NotificationEvent.InstructorApproved e -> dispatch(
                    e.instructorId(), "INSTRUCTOR_ACCOUNT_APPROVED", Map.of());
            default -> log.warn("Unknown notification event type: {}", event.getClass().getSimpleName());
        }
    }

    private void dispatch(Long userId, String eventType, Map<String, Object> payload) {
        try {
            notificationClient.send(new NotificationDispatchRequest(userId, eventType, payload));
        } catch (Exception ex) {
            log.error("notifier.dispatch.failed eventType={} recipientId={}", eventType, userId, ex);
        }
    }
}
