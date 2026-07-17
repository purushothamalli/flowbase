package com.flowbase.engine.auth.v1;

import com.flowbase.engine.auth.dto.AuthenticatedUser;
import com.flowbase.engine.auth.dto.LoginRequest;
import com.flowbase.engine.auth.dto.LoginResponse;
import com.flowbase.engine.auth.dto.RegisterRequest;
import com.flowbase.engine.auth.dto.UserResponse;
import com.flowbase.engine.auth.exception.AuthenticationException;
import com.flowbase.engine.auth.service.AuthService;
import com.flowbase.engine.config.UserContext;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/v1/auth")
@AllArgsConstructor
class AuthV1Controller {
    private final AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        UserResponse response = this.authService.register(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = this.authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        token = token.substring(7);
        this.authService.logout(token);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe() {
        AuthenticatedUser user = UserContext.get();
        if (user == null) throw new AuthenticationException("Access denied: Missing session context");
        UserResponse activeUser = new UserResponse(user.id(), user.email(), user.role());
        return ResponseEntity.ok(activeUser);
    }
}
