package com.flowbase.engine.auth.service;

public interface PasswordEncoder {
    String encode(String rawPassword);
    
    boolean matches(String rawPassword, String encodedPassword);
}
