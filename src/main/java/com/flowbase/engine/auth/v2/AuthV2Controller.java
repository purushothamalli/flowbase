package com.flowbase.engine.auth.v2;

import com.flowbase.engine.auth.dto.AuthenticatedUser;
import com.flowbase.engine.auth.dto.UserResponse;
import com.flowbase.engine.auth.exception.AuthenticationException;
import com.flowbase.engine.config.UserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("v2/auth")
class AuthV2Controller {
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe() {
        AuthenticatedUser user = UserContext.get();
        if (user == null) throw new AuthenticationException("Access denied: Missing session context");
        UserResponse activeUser = new UserResponse(user.id(), user.email(), user.role());
        return ResponseEntity.ok().header("x-FlowBase-Version", "v2").body(activeUser);
    }
}
