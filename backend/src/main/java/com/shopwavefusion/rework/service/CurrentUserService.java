package com.shopwavefusion.rework.service;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.shopwavefusion.rework.api.v1.ApiException;
import com.shopwavefusion.rework.domain.Role;
import com.shopwavefusion.rework.domain.UserEntity;
import com.shopwavefusion.rework.repository.UserRepository;

import org.springframework.http.HttpStatus;

@Service
public class CurrentUserService {
    private final UserRepository users;

    public CurrentUserService(UserRepository users) { this.users = users; }

    public UserEntity require(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required");
        }
        try {
            UUID id = UUID.fromString(authentication.getName());
            UserEntity user = users.findById(id).orElseThrow(() ->
                    new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is invalid"));
            if (!user.isActive()) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is invalid");
            }
            return user;
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is invalid");
        }
    }

    public void requireAdmin(Authentication authentication) {
        if (require(authentication).getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Administrator access is required");
        }
    }
}
