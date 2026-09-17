package com.shopwavefusion.rework.api.v1;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CommonDtos {
    private CommonDtos() {}

    public record ApiFieldError(String field, String message) {}
    public record ProblemDetails(String type, String title, int status, String code, String detail,
                                 String instance, String traceId, List<ApiFieldError> fieldErrors) {}
    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {}

    public record UserResponse(UUID id, String email, String firstName, String lastName,
                               String mobile, String role, Instant createdAt) {}

    public record AddressResponse(UUID id, String firstName, String lastName, String streetAddress,
                                  String city, String department, String postalCode, String mobile,
                                  String country) {}

    public record CategoryResponse(UUID id, String name, UUID parentId,
                                   List<CategoryPathItem> path) {}
    public record CategoryPathItem(UUID id, String name) {}

    public record VariantResponse(UUID id, String label, int stock, boolean active) {}
    public record ProductResponse(UUID id, String title, String description, String brand, String color,
                                  String imageUrl, long priceMinor, long salePriceMinor, String currency,
                                  int discountPercent, int stockTotal, List<VariantResponse> variants,
                                  CategoryResponse category, boolean active, long version,
                                  Instant createdAt, Instant updatedAt) {}

    public record FacetsResponse(List<CategoryResponse> categories, List<String> colors,
                                 List<String> variantLabels, long minPriceMinor, long maxPriceMinor) {}

    public record CartItemResponse(UUID id, UUID variantId, UUID productId, String title, String imageUrl,
                                   String variantLabel, int quantity, int stockAvailable,
                                   long unitPriceMinor, long unitSalePriceMinor, long lineTotalMinor,
                                   boolean available) {}

    public record CartResponse(UUID id, long version, String quoteFingerprint,
                               List<CartItemResponse> items, long subtotalMinor, long discountMinor,
                               long totalMinor, int totalQuantity, String currency) {}

    public record PaymentResponse(String method, String status, String reference) {}
    public record OrderItemResponse(UUID id, UUID productId, UUID variantId, String title,
                                    String imageUrl, String variantLabel, int quantity,
                                    long unitPriceMinor, long unitSalePriceMinor, long lineTotalMinor) {}

    public record OrderResponse(UUID id, String number, String status, long version, Instant createdAt,
                                Instant deliveredAt, AddressResponse shippingAddress,
                                List<OrderItemResponse> items, long subtotalMinor, long discountMinor,
                                long totalMinor, int totalQuantity, String currency,
                                PaymentResponse payment, List<String> allowedTransitions) {}

    public record AdminOrderResponse(UUID id, String number, String status, long version, Instant createdAt,
                                     Instant deliveredAt, AddressResponse shippingAddress,
                                     List<OrderItemResponse> items, long subtotalMinor, long discountMinor,
                                     long totalMinor, int totalQuantity, String currency,
                                     PaymentResponse payment, List<String> allowedTransitions,
                                     UserResponse customer) {}
    public record AdminSummaryResponse(long activeProducts, long ordersPlaced, long ordersConfirmed,
                                       long simulatedSalesMinor, String currency,
                                       Instant periodStart, Instant periodEnd) {}
}
