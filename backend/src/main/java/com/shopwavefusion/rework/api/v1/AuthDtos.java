package com.shopwavefusion.rework.api.v1;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.shopwavefusion.rework.api.v1.CommonDtos.UserResponse;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 72) String password,
            @NotBlank @Pattern(regexp = "(?:\\+591)?[0-9]{8}") String mobile) {}

    public record LoginRequest(@NotBlank @Email String email,
                               @NotBlank @Size(max = 72) String password) {}

    public record ProfileUpdateRequest(@NotBlank @Size(max = 80) String firstName,
                                       @NotBlank @Size(max = 80) String lastName,
                                       @NotBlank @Pattern(regexp = "(?:\\+591)?[0-9]{8}") String mobile) {}

    public record TokenResponse(String accessToken, UserResponse user, Instant expiresAt) {}
}
