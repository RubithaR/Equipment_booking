package com.smartlab.userservice.dto;

import com.smartlab.userservice.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private String department;
    private String phoneNumber;
    private String enNumber;
    private String indexNumber;
    private String nameWithInitial;
    private String uniEmail;

    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getRole(),
                u.getStatus(),
                u.getDepartment(),
                u.getPhoneNumber(),
                u.getEnNumber(),
                u.getIndexNumber(),
                u.getNameWithInitial(),
                u.getUniEmail()
        );
    }
}
