package com.flowbase.engine.auth.service;

import com.flowbase.engine.auth.domain.User;
import com.flowbase.engine.auth.dto.AuthenticatedUser;
import com.flowbase.engine.auth.dto.LoginRequest;
import com.flowbase.engine.auth.dto.LoginResponse;
import com.flowbase.engine.auth.dto.RegisterRequest;
import com.flowbase.engine.auth.dto.UserResponse;
import com.flowbase.engine.auth.exception.AuthenticationException;
import com.flowbase.engine.auth.exception.InvalidCredentialsException;
import com.flowbase.engine.auth.exception.UserAlreadyExistsException;
import com.flowbase.engine.auth.repository.UserRepository;
import com.flowbase.engine.config.TenantContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    
    @Override
    public UserResponse register(RegisterRequest request) {
        if (TenantContext.get() == null) throw new AuthenticationException("Active Tenant context required!");
        if (userRepository.existsByEmail(request.email()))
            throw new UserAlreadyExistsException("Email already registered!");
        User newUser = new User(UUID.randomUUID()
                                    .toString(), TenantContext.get(), request.email(), this.passwordEncoder.encode(request.password()), request.role());
        this.userRepository.save(newUser);
        return new UserResponse(newUser.id(), newUser.email(), newUser.role());
    }
    
    @Override
    public LoginResponse login(LoginRequest request) {
        Optional<User> userExists = this.userRepository.findByEmail(request.email());
        if (userExists.isEmpty()) throw new InvalidCredentialsException("Invalid email or password!");
        User user = userExists.get();
        if (!this.passwordEncoder.matches(request.password(), user.passwordHash()))
            throw new InvalidCredentialsException("Invalid email or password!");
        String token = this.jwtService.generateToken(user);
        return new LoginResponse(token, "Bearer", 60 * 15);
    }
    
    @Override
    public void logout(String token) {
        AuthenticatedUser authenticatedUser = this.jwtService.parseAndValidate(token);
        tokenBlacklistService.revoke(authenticatedUser.jti(), authenticatedUser.expiresAt());
    }
}
