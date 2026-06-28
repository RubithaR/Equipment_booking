package com.smartlab.equipmentservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Subset of user-service's UserResponse — only the fields equipment-service needs
 * to validate cross-service references and display lab/instructor info.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSummary {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private Long facultyId;
    private Long departmentId;
    private String phoneNumber;
}
