package com.smartlab.userservice.dto;

/**
 * The department-wide approvers for a booking's first two stages:
 * the Head of Department (stage 1) and a Lecturer/supervisor (stage 2).
 * Either may be null when the department has no such active user — that
 * stage is then skipped by the booking service.
 */
public record DepartmentApprovalChain(UserResponse hod, UserResponse lecturer) {}
