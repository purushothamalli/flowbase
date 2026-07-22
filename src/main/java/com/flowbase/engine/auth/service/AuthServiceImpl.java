package com.flowbase.engine.auth.service;

import com.flowbase.engine.auth.domain.RefreshToken;
import com.flowbase.engine.auth.domain.User;
import com.flowbase.engine.auth.dto.AuthResult;
import com.flowbase.engine.auth.dto.AuthenticatedUser;
import com.flowbase.engine.auth.dto.LoginRequest;
import com.flowbase.engine.auth.dto.LoginResponse;
import com.flowbase.engine.auth.dto.RegisterRequest;
import com.flowbase.engine.auth.dto.UserResponse;
import com.flowbase.engine.auth.exception.AuthenticationException;
import com.flowbase.engine.auth.exception.InvalidCredentialsException;
import com.flowbase.engine.auth.exception.InvalidTokenException;
import com.flowbase.engine.auth.exception.UserAlreadyExistsException;
import com.flowbase.engine.auth.repository.RefreshTokenRepository;
import com.flowbase.engine.auth.repository.UserRepository;
import com.flowbase.engine.common.service.IdGenerator;
import com.flowbase.engine.config.TenantContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@AllArgsConstructor
class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final IdGenerator idGenerator;
    private final RefreshTokenRepository refreshTokenRepository;
    
    @Override
    public UserResponse register(RegisterRequest request) {
        if (TenantContext.get() == null) throw new AuthenticationException("Active Tenant context required!");
        if (userRepository.existsByTenantIdAndEmail(TenantContext.get(), request.email()))
            throw new UserAlreadyExistsException("Email already registered!");
        User newUser = new User(this.idGenerator.generate(), TenantContext.get(), request.email(), this.passwordEncoder.encode(request.password()), request.role());
        this.userRepository.save(newUser);
        return UserResponse.from(newUser);
    }
    
    @Override
    public AuthResult login(LoginRequest request) {
        Optional<User> userExists = this.userRepository.findByTenantIdAndEmail(TenantContext.get(), request.email());
        if (userExists.isEmpty()) throw new InvalidCredentialsException("Invalid email or password!");
        User user = userExists.get();
        if (!this.passwordEncoder.matches(request.password(), user.passwordHash()))
            throw new InvalidCredentialsException("Invalid email or password!");
        String accessToken = this.jwtService.generateToken(user);
        String refreshToken = this.idGenerator.generate();
        this.refreshTokenRepository.save(new RefreshToken(refreshToken, user.id(), TenantContext.get(),
                Instant.now().plus(Duration.ofDays(7)), false));
        return new AuthResult(accessToken, refreshToken, 15 * 60);
    }
    
    @Override
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResult refresh(String refreshToken) {
        Optional<RefreshToken> tokenExists = this.refreshTokenRepository.findById(refreshToken);
        if (tokenExists.isEmpty()) throw new InvalidTokenException("Invalid refresh Token!");
        RefreshToken token = tokenExists.get();
        if (token.expiresAt().isBefore(Instant.now())) throw new InvalidTokenException("Invalid refresh Token!");
        if (token.revoked()) {
            this.refreshTokenRepository.deleteAllByUserId(token.userId());
            throw new InvalidTokenException("Invalid refresh Token!");
        }
        Optional<User> userExists = this.userRepository.findById(token.userId());
        if (userExists.isEmpty()) throw new InvalidTokenException("Invalid refresh Token!");
        token.revoked(true);
        this.refreshTokenRepository.save(token);
        String accessToken = this.jwtService.generateToken(userExists.get());
        String newRefreshToken = this.idGenerator.generate();
        this.refreshTokenRepository.save(new RefreshToken(newRefreshToken, token.userId(), TenantContext.get(),
                Instant.now().plus(Duration.ofDays(7)), false));
        return new AuthResult(accessToken, newRefreshToken, 15 * 60);
    }
    
    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                AuthenticatedUser authenticatedUser = this.jwtService.parseAndValidate(accessToken);
                tokenBlacklistService.revoke(authenticatedUser.jti(), authenticatedUser.expiresAt());
            } catch (Exception e) {
                // Ignore invalid or expired token during logout
            }
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            Optional<RefreshToken> refreshTokenExists = this.refreshTokenRepository.findById(refreshToken);
            refreshTokenExists.ifPresent(token -> this.refreshTokenRepository.save(token.revoked(true)));
        }
    }
}
