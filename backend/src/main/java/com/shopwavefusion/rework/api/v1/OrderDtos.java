package com.shopwavefusion.rework.api.v1;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

public final class OrderDtos {
    private OrderDtos() {}

    public record AddressInput(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Size(max = 200) String streetAddress,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Size(max = 40) String department,
            @Size(max = 20) String postalCode,
            @NotBlank @Pattern(regexp = "(?:\\+591)?[0-9]{8}") String mobile,
            @NotNull @Pattern(regexp = "BO") String country) {}

    public record CheckoutRequest(@NotNull @Valid AddressInput address,
                                  boolean saveAddress,
                                  @NotBlank @Pattern(regexp = "MOCK") String paymentMethod,
                                  @NotNull @Min(0) Long cartVersion,
                                  @NotBlank String quoteFingerprint) {}

    public record StatusRequest(@NotBlank String status, @Min(0) long version) {}
}
