package com.flowbase.engine.auth.v1;

import com.flowbase.engine.auth.dto.AuthResult;
import com.flowbase.engine.auth.dto.AuthenticatedUser;
import com.flowbase.engine.auth.dto.LoginRequest;
import com.flowbase.engine.auth.dto.LoginResponse;
import com.flowbase.engine.auth.dto.RegisterRequest;
import com.flowbase.engine.auth.dto.UserResponse;
import com.flowbase.engine.auth.exception.AuthenticationException;
import com.flowbase.engine.auth.exception.InvalidTokenException;
import com.flowbase.engine.auth.service.AuthService;
import com.flowbase.engine.config.UserContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
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
        UserResponse response = this.authService.register(request); return ResponseEntity.ok(response);
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(HttpServletResponse response, @RequestBody LoginRequest request) {
        AuthResult authResult = this.authService.login(request); this.setAuthCookies(response, authResult);
        return ResponseEntity.ok(new LoginResponse(authResult.accessToken(), "Bearer", authResult.expiresInSeconds()));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@CookieValue(name = "fb_refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank())
            throw new InvalidTokenException("Refresh Token cookie missing!");
        AuthResult authResult = this.authService.refresh(refreshToken);
        this.setAuthCookies(response, authResult);
        return ResponseEntity.ok(new LoginResponse(authResult.accessToken(), "Bearer", authResult.expiresInSeconds()));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response, @CookieValue(name = "fb_access_token", required = false) String accessToken, @CookieValue(name = "fb_refresh_token", required = false) String refreshToken) {
        this.authService.logout(accessToken, refreshToken);
        ResponseCookie clearAccessCookie = ResponseCookie
                .from("fb_access_token")
                .value("").path("/")
                .maxAge(0)
                .build();
        ResponseCookie clearRefreshCookie = ResponseCookie
                .from("fb_refresh_token")
                .value("")
                .path("/v1/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearAccessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString());
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe() {
        AuthenticatedUser user = UserContext.get();
        if (user == null) throw new AuthenticationException("Access denied: Missing session context");
        UserResponse activeUser = new UserResponse(user.id(), user.email(), user.role());
        return ResponseEntity.ok(activeUser);
    }
    
    private void setAuthCookies(HttpServletResponse response, AuthResult result) {
        ResponseCookie accessCookie = ResponseCookie.from("fb_access_token")
                                                    .value(result.accessToken())
                                                    .httpOnly(true)
                                                    .secure(false)
                                                    .path("/")
                                                    .maxAge(result.expiresInSeconds())
                                                    .sameSite("Lax")
                                                    .build();
        ResponseCookie refreshCookie = ResponseCookie.from("fb_refresh_token")
                                                     .value(result.refreshToken())
                                                     .httpOnly(true)
                                                     .secure(false)
                                                     .path("/v1/auth")
                                                     .maxAge(7 * 24 * 60 * 60)
                                                     .sameSite("Lax")
                                                     .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
}
