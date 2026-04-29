package com.smartlab.bookingservice.notifier;

import com.smartlab.bookingservice.client.NotificationClient;
import com.smartlab.bookingservice.client.UserClient;
import com.smartlab.bookingservice.dto.NotificationDto;
import com.smartlab.bookingservice.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FeignNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(FeignNotifier.class);
    private static final String INSTRUCTOR_ROLE = "INSTRUCTOR";

    private final NotificationClient notificationClient;
    private final UserClient userClient;

    @Override
    public void publish(NotificationEvent event) {
        switch (event) {
            case NotificationEvent.BookingSubmitted e -> {
                deliver(e.studentId(),
                        "Booking submitted",
                        "Your booking #" + e.bookingId() + " for " + e.equipmentName()
                                + " is awaiting instructor approval.",
                        "BOOKING_SUBMITTED", e);
                fanOutToInstructors(
                        "New booking awaiting review",
                        e.studentFullName() + " requested " + e.equipmentName()
                                + " from " + e.startTime() + " to " + e.endTime() + ".",
                        "BOOKING_NEEDS_REVIEW", e);
            }
            case NotificationEvent.BookingApproved e -> deliver(e.studentId(),
                    "Booking approved",
                    "Your booking #" + e.bookingId() + " has been approved by instructor.",
                    "BOOKING_APPROVED", e);
            case NotificationEvent.BookingRejected e -> deliver(e.studentId(),
                    "Booking rejected",
                    "Your booking #" + e.bookingId() + " was rejected by instructor."
                            + (e.reason() != null ? " Reason: " + e.reason() : ""),
                    "BOOKING_REJECTED", e);
            case NotificationEvent.BookingCancelled e -> deliver(e.studentId(),
                    "Booking cancelled",
                    "Your booking #" + e.bookingId() + " has been cancelled.",
                    "BOOKING_CANCELLED", e);
        }
    }

    private void deliver(Long userId, String title, String message, String type, NotificationEvent event) {
        try {
            notificationClient.send(new NotificationDto(userId, title, message, type));
        } catch (Exception ex) {
            log.error("notifier.publish.failed event={} recipientId={}", event, userId, ex);
        }
    }

    private void fanOutToInstructors(String title, String message, String type, NotificationEvent event) {
        List<UserDto> instructors;
        try {
            instructors = userClient.getByRole(INSTRUCTOR_ROLE);
        } catch (Exception ex) {
            log.error("notifier.publish.fanout-lookup-failed event={}", event, ex);
            return;
        }
        for (UserDto inst : instructors) {
            deliver(inst.getId(), title, message, type, event);
        }
    }
}
