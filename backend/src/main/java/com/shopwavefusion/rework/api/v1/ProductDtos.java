package com.shopwavefusion.rework.api.v1;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ProductDtos {
    private ProductDtos() {}

    public record VariantWriteRequest(UUID id, @NotBlank @Size(max = 30) String label,
                                      @Min(0) @NotNull Integer stock, @NotNull Boolean active) {}

    public record ProductWriteRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 4000) String description,
            @NotBlank @Size(max = 80) String brand,
            @NotBlank @Size(max = 40) String color,
            @NotNull UUID categoryId,
            @NotBlank @Size(max = 1000) String imageUrl,
            @Min(1) long priceMinor,
            @Min(1) long salePriceMinor,
            @NotEmpty List<@Valid VariantWriteRequest> variants,
            Boolean active,
            Long version) {}
}
