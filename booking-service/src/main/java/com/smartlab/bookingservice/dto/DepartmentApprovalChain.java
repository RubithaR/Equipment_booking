package com.smartlab.bookingservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * The student's department approvers for stages 1 and 2 — the HoD and a Lecturer.
 * Either may be null when the department has no such active user; that stage is skipped.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DepartmentApprovalChain {
    private UserDto hod;
    private UserDto lecturer;
}
