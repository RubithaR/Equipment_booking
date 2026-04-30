package com.smartlab.bookingservice.repository;

import com.smartlab.bookingservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStudentUserIdOrderByCreatedAtDesc(Long studentUserId);
    List<Booking> findByStudentDepartmentIdOrderByCreatedAtDesc(Long studentDepartmentId);
    List<Booking> findByStateOrderByCreatedAtDesc(String state);
    List<Booking> findByStudentDepartmentIdAndStateOrderByCreatedAtDesc(Long deptId, String state);
}
