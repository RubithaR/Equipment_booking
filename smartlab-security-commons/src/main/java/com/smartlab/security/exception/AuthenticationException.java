package com.smartlab.security.exception;

/** Credentials missing, malformed, or invalid — maps to HTTP 401. */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) { super(message); }
}
