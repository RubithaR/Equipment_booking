package com.smartlab.bookingservice.dto;

import lombok.Data;

@Data
public class UserDto {
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
}
