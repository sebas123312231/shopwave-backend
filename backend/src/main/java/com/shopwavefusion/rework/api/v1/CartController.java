package com.shopwavefusion.rework.api.v1;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopwavefusion.rework.api.v1.CartDtos.AddItemRequest;
import com.shopwavefusion.rework.api.v1.CartDtos.UpdateItemRequest;
import com.shopwavefusion.rework.service.CartService;
import com.shopwavefusion.rework.service.CurrentUserService;

import jakarta.validation.Valid;

import static com.shopwavefusion.rework.api.v1.CommonDtos.CartResponse;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartService carts;
    private final CurrentUserService current;

    public CartController(CartService carts, CurrentUserService current) { this.carts = carts; this.current = current; }

    @GetMapping
    public CartResponse get(Authentication authentication) { return carts.get(current.require(authentication)); }

    @PostMapping("/items")
    public CartResponse add(Authentication authentication, @Valid @RequestBody AddItemRequest request) {
        return carts.add(current.require(authentication), request);
    }

    @PatchMapping("/items/{id}")
    public CartResponse update(Authentication authentication, @PathVariable UUID id,
                               @Valid @RequestBody UpdateItemRequest request) {
        return carts.update(current.require(authentication), id, request);
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> remove(Authentication authentication, @PathVariable UUID id) {
        carts.remove(current.require(authentication), id);
        return ResponseEntity.noContent().build();
    }
}
