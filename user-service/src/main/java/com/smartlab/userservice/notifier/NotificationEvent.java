package com.smartlab.userservice.notifier;

public sealed interface NotificationEvent permits
        NotificationEvent.InstructorApproved,
        NotificationEvent.StudentApproved {

    record InstructorApproved(Long instructorId) implements NotificationEvent {}
    record StudentApproved(Long studentId) implements NotificationEvent {}
}
