package com.shopwavefusion.rework.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopwavefusion.rework.api.v1.ApiException;
import com.shopwavefusion.rework.api.v1.CartDtos.AddItemRequest;
import com.shopwavefusion.rework.api.v1.CartDtos.UpdateItemRequest;
import com.shopwavefusion.rework.domain.CartEntity;
import com.shopwavefusion.rework.domain.CartItemEntity;
import com.shopwavefusion.rework.domain.ProductEntity;
import com.shopwavefusion.rework.domain.ProductVariantEntity;
import com.shopwavefusion.rework.domain.UserEntity;
import com.shopwavefusion.rework.repository.CartItemRepository;
import com.shopwavefusion.rework.repository.CartRepository;
import com.shopwavefusion.rework.repository.ProductVariantRepository;

import static com.shopwavefusion.rework.api.v1.CommonDtos.*;

@Service
public class CartService {
    private final CartRepository carts;
    private final CartItemRepository items;
    private final ProductVariantRepository variants;

    public CartService(CartRepository carts, CartItemRepository items, ProductVariantRepository variants) {
        this.carts = carts;
        this.items = items;
        this.variants = variants;
    }

    @Transactional(readOnly = true)
    public CartResponse get(UserEntity user) {
        CartEntity cart = carts.findByUserId(user.getId()).orElseThrow(() ->
                new ApiException(HttpStatus.CONFLICT, "CART_NOT_INITIALIZED", "Cart is not initialized"));
        return response(cart);
    }

    @Transactional
    public CartResponse add(UserEntity user, AddItemRequest request) {
        if (request.variantId() == null) throw bad("INVALID_VARIANT", "Variant is required");
        CartEntity cart = load(user, true);
        ProductVariantEntity variant = variants.findById(request.variantId()).orElseThrow(() ->
                bad("VARIANT_NOT_FOUND", "Variant not found"));
        requireAvailable(variant);
        CartItemEntity item = cart.getItems().stream().filter(i -> i.getVariant().getId().equals(variant.getId())).findFirst().orElse(null);
        int next = (item == null ? 0 : item.getQuantity()) + request.quantity();
        if (next > 10) throw conflict("CART_ITEM_LIMIT", "A variant is limited to ten units");
        if (next > variant.getStock()) throw conflict("INSUFFICIENT_STOCK", "The requested stock is not available");
        if (item == null) {
            item = new CartItemEntity(); item.setCart(cart); item.setVariant(variant); item.setQuantity(next); cart.getItems().add(item);
        } else item.setQuantity(next);
        cart.setVersion(cart.getVersion() + 1);
        return response(carts.saveAndFlush(cart));
    }

    @Transactional
    public CartResponse update(UserEntity user, UUID id, UpdateItemRequest request) {
        CartEntity cart = load(user, true);
        CartItemEntity item = cart.getItems().stream().filter(i -> i.getId().equals(id)).findFirst().orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", "Cart item not found"));
        requireAvailable(item.getVariant());
        if (request.quantity() > item.getVariant().getStock()) throw conflict("INSUFFICIENT_STOCK", "The requested stock is not available");
        item.setQuantity(request.quantity());
        cart.setVersion(cart.getVersion() + 1);
        return response(carts.saveAndFlush(cart));
    }

    @Transactional
    public void remove(UserEntity user, UUID id) {
        CartEntity cart = load(user, true);
        CartItemEntity item = cart.getItems().stream().filter(i -> i.getId().equals(id)).findFirst().orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", "Cart item not found"));
        cart.getItems().remove(item);
        items.delete(item);
        cart.setVersion(cart.getVersion() + 1);
        carts.saveAndFlush(cart);
    }

    public CartEntity load(UserEntity user, boolean lock) {
        return (lock ? carts.findByUserIdForUpdate(user.getId()) : carts.findByUserId(user.getId())).orElseGet(() -> {
            CartEntity cart = new CartEntity(); cart.setUser(user); return carts.save(cart);
        });
    }

    public CartResponse response(CartEntity cart) {
        var rows = cart.getItems().stream().sorted(Comparator.comparing(CartItemEntity::getId)).map(item -> {
            ProductVariantEntity variant = item.getVariant(); ProductEntity p = variant.getProduct();
            long line = p.getSalePriceMinor() * item.getQuantity();
            boolean available = p.isActive() && variant.isActive() && variant.getStock() >= item.getQuantity();
            return new CartItemResponse(item.getId(), variant.getId(), p.getId(), p.getTitle(), p.getImageUrl(),
                    variant.getLabel(), item.getQuantity(), variant.getStock(), p.getPriceMinor(), p.getSalePriceMinor(), line, available);
        }).toList();
        long subtotal = rows.stream().mapToLong(r -> r.unitPriceMinor() * r.quantity()).sum();
        long total = rows.stream().mapToLong(CartItemResponse::lineTotalMinor).sum();
        return new CartResponse(cart.getId(), cart.getVersion(), fingerprint(cart), rows, subtotal, subtotal - total,
                total, rows.stream().mapToInt(CartItemResponse::quantity).sum(), "BOB");
    }

    public String fingerprint(CartResponse cart) {
        String canonical = cart.items().stream().sorted(Comparator.comparing(CartItemResponse::variantId))
                .map(r -> r.variantId() + ":" + r.quantity() + ":" + r.unitPriceMinor() + ":" + r.unitSalePriceMinor()
                        + ":" + r.stockAvailable())
                .reduce("", (a, b) -> a + "|" + b);
        return digest(canonical);
    }

    private String fingerprint(CartEntity cart) {
        String canonical = cart.getItems().stream().sorted(Comparator.comparing(i -> i.getVariant().getId()))
                .map(i -> {
                    ProductVariantEntity variant = i.getVariant();
                    ProductEntity product = variant.getProduct();
                    return variant.getId() + ":" + i.getQuantity() + ":" + product.getVersion() + ":"
                            + variant.getVersion() + ":" + product.getPriceMinor() + ":" + product.getSalePriceMinor()
                            + ":" + variant.getStock() + ":" + product.isActive() + ":" + variant.isActive();
                })
                .reduce("", (a, b) -> a + "|" + b);
        return digest(canonical);
    }

    private static String digest(String canonical) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    private static void requireAvailable(ProductVariantEntity variant) {
        if (!variant.isActive() || !variant.getProduct().isActive()) throw conflict("PRODUCT_UNAVAILABLE", "Product is unavailable");
    }
    private static ApiException bad(String code, String message) { return new ApiException(HttpStatus.BAD_REQUEST, code, message); }
    private static ApiException conflict(String code, String message) { return new ApiException(HttpStatus.CONFLICT, code, message); }
}
