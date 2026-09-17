package com.shopwavefusion.rework.api.v1;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopwavefusion.rework.api.v1.OrderDtos.CheckoutRequest;
import com.shopwavefusion.rework.service.CurrentUserService;
import com.shopwavefusion.rework.service.OrderService;

import jakarta.validation.Valid;

import static com.shopwavefusion.rework.api.v1.CommonDtos.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orders;
    private final CurrentUserService current;
    public OrderController(OrderService orders, CurrentUserService current) { this.orders = orders; this.current = current; }

    @PostMapping
    public ResponseEntity<OrderResponse> create(Authentication authentication, @Valid @RequestBody CheckoutRequest request,
                                                @RequestHeader("Idempotency-Key") String idempotencyKey) {
        var result = orders.create(current.require(authentication), request, idempotencyKey);
        return ResponseEntity.status(result.replayed() ? 200 : 201).body(result.order());
    }
    @GetMapping
    public PageResponse<OrderResponse> list(Authentication authentication, @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "12") int size) { return orders.userOrders(current.require(authentication), page, size); }
    @GetMapping("/{id}")
    public OrderResponse get(Authentication authentication, @PathVariable UUID id) { return orders.findForUser(current.require(authentication), id); }
}
