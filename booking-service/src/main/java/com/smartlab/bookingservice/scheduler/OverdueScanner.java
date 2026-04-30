package com.smartlab.bookingservice.scheduler;

import com.smartlab.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Flips COLLECTED bookings to OVERDUE once their return date passes.
 * Runs every 5 minutes; first run 30s after boot to give Eureka/Feign time to settle.
 */
@Component
@RequiredArgsConstructor
public class OverdueScanner {

    private static final Logger log = LoggerFactory.getLogger(OverdueScanner.class);

    private final BookingService bookingService;

    @Scheduled(initialDelay = 30_000, fixedDelay = 300_000)
    public void run() {
        try {
            int flipped = bookingService.scanOverdue();
            if (flipped > 0) log.info("OverdueScanner flipped={} bookings", flipped);
        } catch (Exception ex) {
            log.warn("OverdueScanner failed: {}", ex.getMessage());
        }
    }
}
