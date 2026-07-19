package com.flowbase.engine.auth.service;

import com.flowbase.engine.auth.dto.AuthResult;
import com.flowbase.engine.auth.dto.LoginRequest;
import com.flowbase.engine.auth.dto.LoginResponse;
import com.flowbase.engine.auth.dto.RegisterRequest;
import com.flowbase.engine.auth.dto.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    
    AuthResult login(LoginRequest request);
    
    AuthResult refresh(String refreshToken);
    
    void logout(String accessToken, String refreshToken);
}
