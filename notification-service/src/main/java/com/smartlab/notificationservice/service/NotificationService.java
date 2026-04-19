package com.smartlab.notificationservice.service;

import com.smartlab.notificationservice.dto.NotificationRequest;
import com.smartlab.notificationservice.entity.Notification;
import com.smartlab.notificationservice.exception.NotFoundException;
import com.smartlab.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    public Notification create(NotificationRequest request) {
        Notification n = new Notification();
        n.setUserId(request.getUserId());
        n.setTitle(request.getTitle());
        n.setMessage(request.getMessage());
        n.setType(request.getType());
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
