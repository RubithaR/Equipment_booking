package com.smartlab.bookingservice.client;

import com.smartlab.bookingservice.dto.DepartmentApprovalChain;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", contextId = "departmentClient")
public interface DepartmentClient {

    /**
     * The department's first two approvers (HoD, Lecturer) for stages 1 and 2.
     * Either field may be null when the department has no such active user.
     */
    @GetMapping("/api/departments/{id}/approval-chain")
    DepartmentApprovalChain getApprovalChain(@PathVariable("id") Long id);
}
