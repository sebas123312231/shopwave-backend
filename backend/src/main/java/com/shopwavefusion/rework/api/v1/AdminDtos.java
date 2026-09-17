package com.shopwavefusion.rework.api.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class AdminDtos {
    private AdminDtos() {}
    public record ArchiveRequest(@NotNull Boolean active, @NotNull @Min(0) Long version) {}
    public record SummaryQuery(String period) {}
    public record AdminStatusRequest(@NotBlank String status, @NotNull @Min(0) Long version) {}
}
