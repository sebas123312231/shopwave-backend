package com.shopwavefusion.rework.api.v1;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopwavefusion.rework.api.v1.AdminDtos.AdminStatusRequest;
import com.shopwavefusion.rework.api.v1.AdminDtos.ArchiveRequest;
import com.shopwavefusion.rework.api.v1.OrderDtos.StatusRequest;
import com.shopwavefusion.rework.api.v1.ProductDtos.ProductWriteRequest;
import com.shopwavefusion.rework.service.OrderService;
import com.shopwavefusion.rework.service.ProductService;

import jakarta.validation.Valid;

import static com.shopwavefusion.rework.api.v1.CommonDtos.*;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final ProductService products;
    private final OrderService orders;
    public AdminController(ProductService products, OrderService orders) { this.products = products; this.orders = orders; }

    @GetMapping("/summary")
    public AdminSummaryResponse summary() { return orders.summary(); }

    @GetMapping("/products")
    public PageResponse<ProductResponse> products(@RequestParam(required = false) String q,
                                                   @RequestParam(required = false) UUID categoryId,
                                                   @RequestParam(required = false, name = "color") List<String> colors,
                                                   @RequestParam(required = false, name = "variantLabel") List<String> labels,
                                                   @RequestParam(required = false) Long minPriceMinor,
                                                   @RequestParam(required = false) Long maxPriceMinor,
                                                   @RequestParam(required = false) Boolean inStock,
                                                   @RequestParam(required = false) Boolean active,
                                                   @RequestParam(defaultValue = "newest") String sort,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "12") int size) {
        return products.list(q, categoryId, colors, labels, minPriceMinor, maxPriceMinor, inStock, active, sort, page, size, true);
    }

    @GetMapping("/products/{id}")
    public ProductResponse product(@PathVariable UUID id) { return products.findAdmin(id); }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductWriteRequest request) {
        ProductResponse response = products.create(request); return ResponseEntity.created(URI.create("/api/v1/admin/products/" + response.id())).body(response);
    }

    @PutMapping("/products/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductWriteRequest request) { return products.update(id, request); }

    @PatchMapping("/products/{id}/archive")
    public ProductResponse archive(@PathVariable UUID id, @Valid @RequestBody ArchiveRequest request) { return products.archive(id, request.active(), request.version()); }

    @GetMapping("/orders")
    public PageResponse<AdminOrderResponse> orders(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "12") int size,
                                                   @RequestParam(required = false) String status) { return this.orders.adminOrders(page, size, status); }

    @GetMapping("/orders/{id}")
    public AdminOrderResponse order(@PathVariable UUID id) { return orders.adminOrder(id); }

    @PatchMapping("/orders/{id}/status")
    public AdminOrderResponse status(@PathVariable UUID id, @Valid @RequestBody AdminStatusRequest request) {
        return orders.updateStatus(id, new StatusRequest(request.status(), request.version()));
    }
}
