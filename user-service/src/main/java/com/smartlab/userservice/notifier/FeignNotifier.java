package com.smartlab.userservice.notifier;

import com.smartlab.userservice.client.NotificationClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FeignNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(FeignNotifier.class);

    private final NotificationClient notificationClient;

    @Override
    public void publish(NotificationEvent event) {
        switch (event) {
            case NotificationEvent.InstructorApproved e -> deliver(e.instructorId(),
                    "Instructor account approved",
                    "Your account has been approved by admin. You can now log in.",
                    "ACCOUNT_APPROVED", e);
        }
    }

    private void deliver(Long userId, String title, String message, String type, NotificationEvent event) {
        try {
            notificationClient.send(Map.of(
                    "userId", userId,
                    "title", title,
                    "message", message,
                    "type", type));
        } catch (Exception ex) {
            log.error("notifier.publish.failed event={} recipientId={}", event, userId, ex);
        }
    }
}
