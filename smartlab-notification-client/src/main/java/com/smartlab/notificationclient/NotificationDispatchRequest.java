package com.smartlab.notificationclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** Wire shape sent to notification-service. {@code eventType} selects the template; {@code payload} fills the slots. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDispatchRequest {
    private Long userId;
    private String eventType;
    private Map<String, Object> payload;
}
