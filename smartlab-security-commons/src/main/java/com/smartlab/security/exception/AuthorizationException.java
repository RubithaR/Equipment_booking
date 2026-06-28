package com.smartlab.security.exception;

/** Caller is authenticated but lacks the required role or relationship — maps to HTTP 403. */
public class AuthorizationException extends RuntimeException {
    public AuthorizationException(String message) { super(message); }
}
