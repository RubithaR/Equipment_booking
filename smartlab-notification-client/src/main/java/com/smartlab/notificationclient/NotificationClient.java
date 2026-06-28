package com.smartlab.notificationclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", contextId = "notificationClient")
public interface NotificationClient {

    @PostMapping("/api/notifications")
    Object send(@RequestBody NotificationDispatchRequest request);
}
