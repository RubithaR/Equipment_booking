package com.smartlab.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Role is required")
    private String role; // STUDENT or INSTRUCTOR (admin not registerable)

    private String department;
    private String phoneNumber;

    // Student-only fields (ignored for instructor)
    private String enNumber;
    private String indexNumber;
    private String nameWithInitial;
    private String uniEmail;
}
