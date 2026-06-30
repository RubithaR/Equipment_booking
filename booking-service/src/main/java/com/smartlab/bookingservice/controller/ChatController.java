package com.smartlab.bookingservice.controller;

import com.smartlab.bookingservice.dto.ConversationResponse;
import com.smartlab.bookingservice.dto.MessageResponse;
import com.smartlab.bookingservice.dto.SendMessageRequest;
import com.smartlab.bookingservice.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Student ↔ instructor chat. Threads are opened by the approval flow; these
 * endpoints let either participant list their threads, read one, and post to it.
 * The current user is always taken from the JWT, never from the request body.
 */
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** My chat threads (as student or instructor), newest activity first. */
    @GetMapping
    public ResponseEntity<List<ConversationResponse>> myConversations() {
        return ResponseEntity.ok(chatService.listMine());
    }

    /** Full message history of one thread; marks it read for me. */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> messages(@PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.getMessages(conversationId));
    }

    /** Post a message to one thread. */
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageResponse> send(@PathVariable Long conversationId,
                                                @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(conversationId, request.getBody()));
    }

    /** Edit one of my own messages. */
    @PatchMapping("/{conversationId}/messages/{messageId}")
    public ResponseEntity<MessageResponse> edit(@PathVariable Long conversationId,
                                                @PathVariable Long messageId,
                                                @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(chatService.editMessage(conversationId, messageId, request.getBody()));
    }

    /** Delete one of my own messages. */
    @DeleteMapping("/{conversationId}/messages/{messageId}")
    public ResponseEntity<Void> delete(@PathVariable Long conversationId,
                                       @PathVariable Long messageId) {
        chatService.deleteMessage(conversationId, messageId);
        return ResponseEntity.noContent().build();
    }
}
