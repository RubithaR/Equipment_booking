package com.smartlab.bookingservice.service;

import com.smartlab.bookingservice.client.UserClient;
import com.smartlab.bookingservice.dto.ConversationResponse;
import com.smartlab.bookingservice.dto.MessageResponse;
import com.smartlab.bookingservice.dto.UserDto;
import com.smartlab.bookingservice.entity.Booking;
import com.smartlab.bookingservice.entity.ChatConversation;
import com.smartlab.bookingservice.entity.ChatMessage;
import com.smartlab.bookingservice.notifier.NotificationEvent;
import com.smartlab.bookingservice.repository.BookingRepository;
import com.smartlab.bookingservice.repository.ChatConversationRepository;
import com.smartlab.bookingservice.repository.ChatMessageRepository;
import com.smartlab.notificationclient.Notifier;
import com.smartlab.security.CurrentUser;
import com.smartlab.security.UserContext;
import com.smartlab.security.exception.AuthorizationException;
import com.smartlab.security.exception.BadRequestException;
import com.smartlab.security.exception.ConflictException;
import com.smartlab.security.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Permanent student ↔ instructor chat, scoped to a booking. A thread is opened
 * by the approval flow (see {@code TransitionEngine}) the moment an instructor
 * approves a line, then both parties read and post messages here. Authorization
 * is local: only the booking's student and the assigned instructor may take part.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final BookingRepository bookingRepository;
    private final UserClient userClient;
    private final Notifier<NotificationEvent> notifier;

    // ===== Opening (called by the approval flow) =====

    /**
     * Idempotently open the thread for an instructor on a booking. Safe to call on
     * every approval — returns the existing thread if one is already open, and is a
     * no-op when the instructor and student are the same person (e.g. an HoD who is
     * also the handler) since chatting with yourself makes no sense.
     */
    @Transactional
    public ChatConversation openConversation(Long bookingId, Long studentUserId, Long instructorUserId) {
        if (bookingId == null || studentUserId == null || instructorUserId == null) return null;
        if (studentUserId.equals(instructorUserId)) return null;
        return conversations.findByBookingIdAndInstructorUserId(bookingId, instructorUserId)
                .orElseGet(() -> {
                    ChatConversation c = new ChatConversation();
                    c.setBookingId(bookingId);
                    c.setStudentUserId(studentUserId);
                    c.setInstructorUserId(instructorUserId);
                    c.setCreatedAt(Instant.now());
                    log.info("chat.open booking={} student={} instructor={}",
                            bookingId, studentUserId, instructorUserId);
                    return conversations.save(c);
                });
    }

    // ===== Reads =====

    /** Every thread the current user is part of, newest activity first, with unread counts. */
    @Transactional(readOnly = true)
    public List<ConversationResponse> listMine() {
        Long me = CurrentUser.require().userId();
        List<ChatConversation> mine = conversations.findByStudentUserIdOrInstructorUserId(me, me);

        List<ConversationResponse> out = new ArrayList<>();
        for (ChatConversation c : mine) {
            boolean iAmStudent = me.equals(c.getStudentUserId());
            Long otherId = iAmStudent ? c.getInstructorUserId() : c.getStudentUserId();
            UserDto other = safeUser(otherId);
            Booking booking = bookingRepository.findById(c.getBookingId()).orElse(null);
            ChatMessage last = messages.findFirstByConversationIdOrderByCreatedAtDesc(c.getId());
            Instant myLastRead = iAmStudent ? c.getStudentLastReadAt() : c.getInstructorLastReadAt();
            long unread = messages.countByConversationIdAndSenderUserIdNotAndCreatedAtAfter(
                    c.getId(), me, myLastRead != null ? myLastRead : Instant.EPOCH);

            out.add(new ConversationResponse(
                    c.getId(),
                    c.getBookingId(),
                    booking != null ? booking.getProjectName() : ("Booking #" + c.getBookingId()),
                    otherId,
                    other != null ? other.getFullName() : ("User #" + otherId),
                    other != null ? other.getRole() : null,
                    last != null ? last.getBody() : null,
                    last != null ? last.getCreatedAt() : c.getLastMessageAt(),
                    unread,
                    iAmStudent));
        }
        out.sort(Comparator.comparing(ConversationResponse::lastMessageAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    /** Full thread, oldest first. Opening it marks everything read for the viewer. */
    @Transactional
    public List<MessageResponse> getMessages(Long conversationId) {
        Long me = CurrentUser.require().userId();
        ChatConversation c = requireParticipant(conversationId, me);
        markRead(c, me);
        conversations.save(c);
        return messages.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(m -> MessageResponse.from(m, me))
                .toList();
    }

    // ===== Write =====

    @Transactional
    public MessageResponse sendMessage(Long conversationId, String body) {
        Long me = CurrentUser.require().userId();
        if (body == null || body.isBlank()) {
            throw new BadRequestException("Message cannot be empty");
        }
        ChatConversation c = requireParticipant(conversationId, me);

        ChatMessage m = new ChatMessage();
        m.setConversationId(conversationId);
        m.setSenderUserId(me);
        m.setBody(body.trim());
        m.setCreatedAt(Instant.now());
        ChatMessage saved = messages.save(m);

        c.setLastMessageAt(saved.getCreatedAt());
        markRead(c, me); // sending implies you've seen everything up to now
        conversations.save(c);

        notifyOther(c, me, saved.getBody());
        return MessageResponse.from(saved, me);
    }

    /** Edit the text of a message — only the sender may do this. Stamps {@code editedAt}. */
    @Transactional
    public MessageResponse editMessage(Long conversationId, Long messageId, String body) {
        Long me = CurrentUser.require().userId();
        if (body == null || body.isBlank()) {
            throw new BadRequestException("Message cannot be empty");
        }
        ChatMessage m = requireOwnMessage(conversationId, messageId, me);
        m.setBody(body.trim());
        m.setEditedAt(Instant.now());
        return MessageResponse.from(messages.save(m), me);
    }

    /** Delete a message — only the sender may do this. Hard delete. */
    @Transactional
    public void deleteMessage(Long conversationId, Long messageId) {
        Long me = CurrentUser.require().userId();
        ChatMessage m = requireOwnMessage(conversationId, messageId, me);
        messages.delete(m);
    }

    // ===== helpers =====

    private ChatConversation requireParticipant(Long conversationId, Long me) {
        ChatConversation c = conversations.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
        if (!me.equals(c.getStudentUserId()) && !me.equals(c.getInstructorUserId())) {
            throw new AuthorizationException("You are not a participant in this conversation");
        }
        return c;
    }

    /** Load a message and assert the current user owns it and it belongs to this thread. */
    private ChatMessage requireOwnMessage(Long conversationId, Long messageId, Long me) {
        ChatMessage m = messages.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found: " + messageId));
        if (!m.getConversationId().equals(conversationId)) {
            throw new ConflictException("Message " + messageId + " does not belong to this conversation");
        }
        if (!me.equals(m.getSenderUserId())) {
            throw new AuthorizationException("You can only edit or delete your own messages");
        }
        return m;
    }

    private void markRead(ChatConversation c, Long me) {
        Instant now = Instant.now();
        if (me.equals(c.getStudentUserId())) c.setStudentLastReadAt(now);
        else c.setInstructorLastReadAt(now);
    }

    private void notifyOther(ChatConversation c, Long sender, String body) {
        Long recipient = sender.equals(c.getStudentUserId()) ? c.getInstructorUserId() : c.getStudentUserId();
        try {
            UserDto me = safeUser(sender);
            String senderName = me != null ? me.getFullName() : "Someone";
            notifier.publish(new NotificationEvent.ChatMessageSent(
                    recipient, c.getBookingId(), senderName, preview(body)));
        } catch (Exception ex) {
            log.warn("chat.notify.failed conversation={} recipient={}: {}",
                    c.getId(), recipient, ex.getMessage());
        }
    }

    private static String preview(String body) {
        String s = body.strip();
        return s.length() <= 80 ? s : s.substring(0, 79) + "…";
    }

    private UserDto safeUser(Long id) {
        if (id == null) return null;
        try { return userClient.getUserById(id); }
        catch (Exception ex) { log.warn("user lookup failed id={}: {}", id, ex.getMessage()); return null; }
    }
}
