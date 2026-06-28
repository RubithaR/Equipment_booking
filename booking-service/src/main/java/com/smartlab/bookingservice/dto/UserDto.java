package com.smartlab.bookingservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDto {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private Long facultyId;
    private Long departmentId;
    private String phoneNumber;
    private String enNumber;
    private String indexNumber;
    private String nameWithInitial;
    private String uniEmail;
}
