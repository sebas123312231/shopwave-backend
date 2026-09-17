package com.shopwavefusion.rework.service;

import java.time.Instant;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopwavefusion.rework.api.v1.ApiException;
import com.shopwavefusion.rework.api.v1.AuthDtos.LoginRequest;
import com.shopwavefusion.rework.api.v1.AuthDtos.RegisterRequest;
import com.shopwavefusion.rework.api.v1.AuthDtos.TokenResponse;
import com.shopwavefusion.rework.api.v1.CommonDtos.AddressResponse;
import com.shopwavefusion.rework.api.v1.CommonDtos.UserResponse;
import com.shopwavefusion.rework.config.JwtTokenProvider;
import com.shopwavefusion.rework.domain.CartEntity;
import com.shopwavefusion.rework.domain.UserEntity;
import com.shopwavefusion.rework.repository.CartRepository;
import com.shopwavefusion.rework.repository.UserRepository;

@Service
public class AuthService {
    private final UserRepository users;
    private final CartRepository carts;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokens;

    public AuthService(UserRepository users, CartRepository carts, PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokens) {
        this.users = users;
        this.carts = carts;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.findByEmail(email).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_IN_USE", "Email is already registered");
        }
        Instant now = Instant.now();
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setMobile(request.mobile().trim());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        UserEntity saved = users.save(user);
        CartEntity cart = new CartEntity();
        cart.setUser(saved);
        carts.save(cart);
        return user(saved);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        UserEntity user = users.findByEmail(normalizeEmail(request.email())).orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password"));
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }
        JwtTokenProvider.TokenData token = tokens.issue(user);
        return new TokenResponse(token.value(), user(user), token.expiresAt());
    }

    @Transactional
    public void logout(UserEntity user) {
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setUpdatedAt(Instant.now());
        users.save(user);
    }

    public UserResponse user(UserEntity user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getMobile(), user.getRole().name(), user.getCreatedAt());
    }

    public static String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
