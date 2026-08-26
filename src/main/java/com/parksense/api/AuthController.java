package com.parksense.api;

import com.parksense.api.dto.AuthDtos.AuthResponse;
import com.parksense.api.dto.AuthDtos.LoginRequest;
import com.parksense.api.dto.AuthDtos.MeResponse;
import com.parksense.auth.PasswordHasher;
import com.parksense.auth.RequestRoles;
import com.parksense.auth.TokenService;
import com.parksense.auth.User;
import com.parksense.store.UserStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Session endpoints: credentials in, bearer token out.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserStore users;
    private final TokenService tokens;

    public AuthController(UserStore users, TokenService tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginRequest request) {
        return users.find(request.username())
                .filter(user -> PasswordHasher.verify(
                        request.password() == null ? new char[0] : request.password().toCharArray(),
                        user.passwordHash()))
                .<ResponseEntity<Object>>map(user -> ResponseEntity.ok(toResponse(user)))
                .orElseGet(() -> ResponseEntity.status(401)
                        .body(Map.of("error", "Invalid username or password")));
    }

    @GetMapping("/me")
    public ResponseEntity<Object> me() {
        return RequestRoles.current()
                .<ResponseEntity<Object>>map(user -> ResponseEntity.ok(
                        new MeResponse(user.username(), user.fullName(), user.role().name())))
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "Not signed in")));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String header) {
        if (header != null && header.startsWith("Bearer ")) {
            tokens.invalidate(header.substring(7));
        }
        return Map.of("ok", true);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(tokens.issue(user), user.username(),
                user.fullName(), user.role().name());
    }
}
