package com.shopwavefusion.rework.api.v1;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopwavefusion.rework.config.AuthRateLimiter;

import com.shopwavefusion.rework.api.v1.AuthDtos.LoginRequest;
import com.shopwavefusion.rework.api.v1.AuthDtos.RegisterRequest;
import com.shopwavefusion.rework.api.v1.AuthDtos.TokenResponse;
import com.shopwavefusion.rework.api.v1.CommonDtos.UserResponse;
import com.shopwavefusion.rework.service.AuthService;
import com.shopwavefusion.rework.service.CurrentUserService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final CurrentUserService current;
    private final AuthRateLimiter rateLimiter;

    public AuthController(AuthService auth, CurrentUserService current, AuthRateLimiter rateLimiter) {
        this.auth = auth;
        this.current = current;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(HttpServletRequest servletRequest, @Valid @RequestBody RegisterRequest request) {
        rateLimiter.check(servletRequest, "register");
        UserResponse response = auth.register(request);
        rateLimiter.success(servletRequest, "register");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public TokenResponse login(HttpServletRequest servletRequest, @Valid @RequestBody LoginRequest request) {
        rateLimiter.check(servletRequest, "login");
        TokenResponse response = auth.login(request);
        rateLimiter.success(servletRequest, "login");
        return response;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        auth.logout(current.require(authentication));
        return ResponseEntity.noContent().build();
    }
}
