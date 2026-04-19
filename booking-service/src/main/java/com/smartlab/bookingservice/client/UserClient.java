package com.smartlab.bookingservice.client;

import com.smartlab.bookingservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);

    // Public list of users by role (used to notify all instructors when a booking is submitted)
    @GetMapping("/api/users/by-role/{role}")
    List<UserDto> getByRole(@PathVariable("role") String role);
}
