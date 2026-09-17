package com.shopwavefusion.rework.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopwavefusion.rework.api.v1.ApiException;
import com.shopwavefusion.rework.api.v1.AuthDtos;
import com.shopwavefusion.rework.api.v1.CommonDtos;
import com.shopwavefusion.rework.api.v1.OrderDtos.AddressInput;
import com.shopwavefusion.rework.api.v1.OrderDtos.CheckoutRequest;
import com.shopwavefusion.rework.api.v1.OrderDtos.StatusRequest;
import com.shopwavefusion.rework.domain.AddressEntity;
import com.shopwavefusion.rework.domain.CartEntity;
import com.shopwavefusion.rework.domain.CartItemEntity;
import com.shopwavefusion.rework.domain.OrderEntity;
import com.shopwavefusion.rework.domain.OrderItemEntity;
import com.shopwavefusion.rework.domain.OrderStatus;
import com.shopwavefusion.rework.domain.PaymentMethod;
import com.shopwavefusion.rework.domain.PaymentStatus;
import com.shopwavefusion.rework.domain.ProductVariantEntity;
import com.shopwavefusion.rework.domain.UserEntity;
import com.shopwavefusion.rework.repository.AddressRepository;
import com.shopwavefusion.rework.repository.CartRepository;
import com.shopwavefusion.rework.repository.OrderRepository;
import com.shopwavefusion.rework.repository.ProductRepository;
import com.shopwavefusion.rework.repository.ProductVariantRepository;

import static com.shopwavefusion.rework.api.v1.CommonDtos.*;

@Service
public class OrderService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/La_Paz");
    private final OrderRepository orders;
    private final CartRepository carts;
    private final ProductVariantRepository variants;
    private final ProductRepository products;
    private final AddressRepository addresses;
    private final CartService cartService;
    private final AuthService auth;

    public OrderService(OrderRepository orders, CartRepository carts, ProductVariantRepository variants,
                        ProductRepository products, AddressRepository addresses, CartService cartService,
                        AuthService auth) {
        this.orders = orders; this.carts = carts; this.variants = variants; this.products = products;
        this.addresses = addresses; this.cartService = cartService; this.auth = auth;
    }

    @Transactional
    public CreateResult create(UserEntity user, CheckoutRequest request, String idempotencyKey) {
        try {
            UUID.fromString(idempotencyKey);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key must be a UUID");
        }
        String hash = hash(request);
        CartEntity cart = carts.findByUserIdForUpdate(user.getId()).orElseThrow(() ->
                new ApiException(HttpStatus.CONFLICT, "EMPTY_CART", "Cart is empty"));
        OrderEntity previous = orders.findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey).orElse(null);
        if (previous != null) {
            if (!previous.getRequestHash().equals(hash)) throw conflict("IDEMPOTENCY_CONFLICT", "Idempotency key was used for another request");
            return new CreateResult(order(previous), true);
        }
        if (cart.getItems().isEmpty()) throw conflict("EMPTY_CART", "Cart is empty");
        if (cart.getVersion() != request.cartVersion()) throw conflict("CART_CHANGED", "Cart changed; review it before checkout");
        CartResponse cartResponse = cartService.response(cart);
        if (!cartResponse.quoteFingerprint().equals(request.quoteFingerprint())) throw conflict("CART_CHANGED", "Prices or stock changed; review the cart");

        List<UUID> ids = cart.getItems().stream().map(i -> i.getVariant().getId()).sorted().toList();
        List<ProductVariantEntity> locked = variants.findAllForUpdate(ids);
        Map<UUID, ProductVariantEntity> byId = new HashMap<>(); locked.forEach(v -> byId.put(v.getId(), v));
        OrderEntity order = new OrderEntity();
        order.setIdempotencyKey(idempotencyKey); order.setRequestHash(hash); order.setUser(user);
        order.setNumber("SW-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase());
        order.setStatus(OrderStatus.PLACED); order.setCreatedAt(Instant.now()); order.setPaymentMethod(PaymentMethod.MOCK);
        order.setPaymentStatus(PaymentStatus.SIMULATED); order.setPaymentReference("mock_" + UUID.randomUUID());
        copyAddress(order, request.address());
        long subtotal = 0; long total = 0; int quantity = 0;
        for (CartItemEntity cartItem : cart.getItems()) {
            ProductVariantEntity variant = byId.get(cartItem.getVariant().getId());
            if (variant == null || !variant.isActive() || !variant.getProduct().isActive()) throw conflict("PRODUCT_UNAVAILABLE", "A cart item is unavailable");
            if (cartItem.getQuantity() < 1 || cartItem.getQuantity() > 10 || variant.getStock() < cartItem.getQuantity()) throw conflict("INSUFFICIENT_STOCK", "Stock changed; review the cart");
            var product = variant.getProduct();
            variant.setStock(variant.getStock() - cartItem.getQuantity());
            long line = product.getSalePriceMinor() * cartItem.getQuantity();
            long regular = product.getPriceMinor() * cartItem.getQuantity();
            OrderItemEntity item = new OrderItemEntity(); item.setOrder(order); item.setProductId(product.getId()); item.setVariantId(variant.getId());
            item.setTitle(product.getTitle()); item.setImageUrl(product.getImageUrl()); item.setVariantLabel(variant.getLabel());
            item.setQuantity(cartItem.getQuantity()); item.setUnitPriceMinor(product.getPriceMinor()); item.setUnitSalePriceMinor(product.getSalePriceMinor()); item.setLineTotalMinor(line);
            order.getItems().add(item); subtotal += regular; total += line; quantity += cartItem.getQuantity();
        }
        if (request.saveAddress()) saveAddress(user, request.address());
        order.setSubtotalMinor(subtotal); order.setDiscountMinor(subtotal - total); order.setTotalMinor(total); order.setTotalQuantity(quantity); order.setCurrency("BOB");
        cart.getItems().clear(); cart.setVersion(cart.getVersion() + 1); carts.save(cart);
        return new CreateResult(order(orders.save(order)), false);
    }

    public record CreateResult(OrderResponse order, boolean replayed) {}

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> userOrders(UserEntity user, int page, int size) {
        Page<OrderEntity> result = orders.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 48)));
        return new PageResponse<>(result.getContent().stream().map(this::order).toList(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public OrderResponse findForUser(UserEntity user, UUID id) {
        return order(orders.findByIdAndUserId(id, user.getId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found")));
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderResponse> adminOrders(int page, int size, String status) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(size, 1), 48);
        if (status != null) {
            try { OrderStatus.valueOf(status); } catch (IllegalArgumentException ex) { throw bad("INVALID_STATUS", "Unknown order status"); }
        }
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<OrderEntity> result = status == null
                ? orders.findAllByOrderByCreatedAtDesc(pageable)
                : orders.findByStatusOrderByCreatedAtDesc(OrderStatus.valueOf(status), pageable);
        return new PageResponse<>(result.getContent().stream().map(this::adminOrder).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminOrderResponse adminOrder(UUID id) {
        OrderEntity order = orders.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found"));
        return adminOrder(order);
    }

    @Transactional
    public AdminOrderResponse updateStatus(UUID id, StatusRequest request) {
        OrderEntity order = orders.findForUpdate(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found"));
        if (order.getVersion() != request.version()) throw conflict("STALE_ORDER", "Order changed; reload before updating");
        OrderStatus next;
        try { next = OrderStatus.valueOf(request.status()); } catch (IllegalArgumentException ex) { throw bad("INVALID_STATUS", "Unknown order status"); }
        if (order.getStatus() == next) return adminOrder(order);
        if (!allowed(order.getStatus(), next)) throw conflict("INVALID_TRANSITION", "Order status transition is not allowed");
        if (next == OrderStatus.CANCELLED) restoreStock(order);
        order.setStatus(next); if (next == OrderStatus.DELIVERED) order.setDeliveredAt(Instant.now()); if (next == OrderStatus.CANCELLED) order.setPaymentStatus(PaymentStatus.VOIDED);
        OrderEntity saved = orders.saveAndFlush(order); return adminOrder(saved);
    }

    @Transactional(readOnly = true)
    public AdminSummaryResponse summary() {
        Instant end = Instant.now(); Instant start = LocalDate.now(BUSINESS_ZONE).withDayOfMonth(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        List<OrderEntity> all = orders.findAll(); long sales = all.stream().filter(o -> !o.getStatus().equals(OrderStatus.CANCELLED) && o.getCreatedAt().isAfter(start)).mapToLong(OrderEntity::getTotalMinor).sum();
        return new AdminSummaryResponse(products.countByActiveTrue(), all.stream().filter(o -> o.getStatus() == OrderStatus.PLACED).count(), all.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED).count(), sales, "BOB", start, end);
    }

    public OrderResponse order(OrderEntity order) {
        AddressResponse address = new AddressResponse(null, order.getShippingFirstName(), order.getShippingLastName(), order.getShippingStreet(), order.getShippingCity(), order.getShippingDepartment(), order.getShippingPostalCode(), order.getShippingMobile(), order.getShippingCountry());
        List<OrderItemResponse> items = order.getItems().stream().map(i -> new OrderItemResponse(i.getId(), i.getProductId(), i.getVariantId(), i.getTitle(), i.getImageUrl(), i.getVariantLabel(), i.getQuantity(), i.getUnitPriceMinor(), i.getUnitSalePriceMinor(), i.getLineTotalMinor())).toList();
        return new OrderResponse(order.getId(), order.getNumber(), order.getStatus().name(), order.getVersion(), order.getCreatedAt(), order.getDeliveredAt(), address, items, order.getSubtotalMinor(), order.getDiscountMinor(), order.getTotalMinor(), order.getTotalQuantity(), order.getCurrency(), new PaymentResponse(order.getPaymentMethod().name(), order.getPaymentStatus().name(), order.getPaymentReference()), allowedTransitions(order.getStatus()));
    }

    private AdminOrderResponse adminOrder(OrderEntity entity) {
        OrderResponse base = order(entity);
        return new AdminOrderResponse(base.id(), base.number(), base.status(), base.version(), base.createdAt(),
                base.deliveredAt(), base.shippingAddress(), base.items(), base.subtotalMinor(), base.discountMinor(),
                base.totalMinor(), base.totalQuantity(), base.currency(), base.payment(), base.allowedTransitions(),
                auth.user(entity.getUser()));
    }

    private void restoreStock(OrderEntity order) {
        List<UUID> ids = order.getItems().stream().map(OrderItemEntity::getVariantId).sorted().toList();
        Map<UUID, ProductVariantEntity> locked = new HashMap<>(); variants.findAllForUpdate(ids).forEach(v -> locked.put(v.getId(), v));
        for (OrderItemEntity item : order.getItems()) { ProductVariantEntity variant = locked.get(item.getVariantId()); if (variant != null) variant.setStock(variant.getStock() + item.getQuantity()); }
    }
    private void saveAddress(UserEntity user, AddressInput input) {
        if (addresses.countByUserId(user.getId()) >= 20) throw bad("ADDRESS_LIMIT", "Address book is full");
        AddressEntity address = new AddressEntity(); address.setUser(user); address.setFirstName(input.firstName()); address.setLastName(input.lastName()); address.setStreetAddress(input.streetAddress()); address.setCity(input.city()); address.setDepartment(input.department()); address.setPostalCode(input.postalCode()); address.setMobile(input.mobile()); address.setCountry(input.country()); addresses.save(address);
    }
    private void copyAddress(OrderEntity o, AddressInput a) { o.setShippingFirstName(a.firstName()); o.setShippingLastName(a.lastName()); o.setShippingStreet(a.streetAddress()); o.setShippingCity(a.city()); o.setShippingDepartment(a.department()); o.setShippingPostalCode(a.postalCode()); o.setShippingMobile(a.mobile()); o.setShippingCountry(a.country()); }
    private static List<String> allowedTransitions(OrderStatus status) { return switch (status) { case PLACED -> List.of("CONFIRMED", "CANCELLED"); case CONFIRMED -> List.of("SHIPPED", "CANCELLED"); case SHIPPED -> List.of("DELIVERED"); default -> List.of(); }; }
    private static boolean allowed(OrderStatus from, OrderStatus to) { return allowedTransitions(from).contains(to.name()); }
    private static ApiException bad(String c, String m) { return new ApiException(HttpStatus.BAD_REQUEST, c, m); }
    private static ApiException conflict(String c, String m) { return new ApiException(HttpStatus.CONFLICT, c, m); }
    private static String hash(CheckoutRequest r) { try { String s = r.address().toString() + r.saveAddress() + r.paymentMethod() + r.cartVersion() + r.quoteFingerprint(); return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }
}
