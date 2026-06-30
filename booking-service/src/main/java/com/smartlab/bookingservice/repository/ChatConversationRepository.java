package com.smartlab.bookingservice.repository;

import com.smartlab.bookingservice.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    /** The single thread for this instructor on this booking, if it has been opened. */
    Optional<ChatConversation> findByBookingIdAndInstructorUserId(Long bookingId, Long instructorUserId);

    /** Every thread the given user takes part in — they are either the student or the instructor. */
    List<ChatConversation> findByStudentUserIdOrInstructorUserId(Long studentUserId, Long instructorUserId);
}
