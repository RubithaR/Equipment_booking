package com.smartlab.notificationservice.service;

import com.smartlab.notificationclient.NotificationDispatchRequest;
import com.smartlab.notificationservice.entity.Notification;
import com.smartlab.notificationservice.repository.NotificationRepository;
import com.smartlab.security.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final MessageRenderer renderer;

    public Notification dispatch(NotificationDispatchRequest request) {
        MessageRenderer.Rendered rendered = renderer.render(request);
        Notification n = new Notification();
        n.setUserId(request.getUserId());
        n.setTitle(rendered.title());
        n.setMessage(rendered.message());
        n.setType(request.getEventType());
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        return repository.save(n);
    }

    public List<Notification> getAll() {
        return repository.findAll();
    }

    public Notification getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found with id: " + id));
    }

    public List<Notification> getByUserId(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadByUserId(Long userId) {
        return repository.findByUserIdAndReadFalse(userId);
    }

    public Notification markAsRead(Long id) {
        Notification n = getById(id);
        n.setRead(true);
        return repository.save(n);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Notification not found with id: " + id);
        }
        repository.deleteById(id);
    }
}
