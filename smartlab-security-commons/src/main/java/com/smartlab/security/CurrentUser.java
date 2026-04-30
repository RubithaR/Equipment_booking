package com.smartlab.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}

    public static UserContext get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserContext ctx)) return null;
        return ctx;
    }

    public static UserContext require() {
        UserContext ctx = get();
        if (ctx == null) throw new IllegalStateException("No authenticated user on the security context");
        return ctx;
    }
}
