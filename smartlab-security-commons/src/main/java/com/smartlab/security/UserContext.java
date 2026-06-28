package com.smartlab.security;

public record UserContext(Long userId, String email, String role, Long facultyId, Long departmentId) {
    public boolean hasRole(String r)        { return role != null && role.equals(r); }
    public boolean hasAnyRole(String... rs) {
        if (role == null) return false;
        for (String r : rs) if (role.equals(r)) return true;
        return false;
    }
}
