package com.smartlab.userservice.notifier;

public sealed interface NotificationEvent permits NotificationEvent.InstructorApproved {

    record InstructorApproved(Long instructorId) implements NotificationEvent {}
}
