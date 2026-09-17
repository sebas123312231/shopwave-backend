package com.shopwavefusion.rework.api.v1;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class CartDtos {
    private CartDtos() {}
    public record AddItemRequest(@NotNull UUID variantId, @Min(1) @Max(10) int quantity) {}
    public record UpdateItemRequest(@Min(1) @Max(10) int quantity) {}
}
