package com.shopwavefusion.rework.api.v1;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopwavefusion.rework.service.ProductService;

import static com.shopwavefusion.rework.api.v1.CommonDtos.*;

@RestController
@RequestMapping("/api/v1")
public class ProductController {
    private final ProductService products;

    public ProductController(ProductService products) { this.products = products; }

    @GetMapping("/products")
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false, name = "color") List<String> colors,
            @RequestParam(required = false, name = "variantLabel") List<String> labels,
            @RequestParam(required = false) Long minPriceMinor,
            @RequestParam(required = false) Long maxPriceMinor,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return products.list(q, categoryId, colors, labels, minPriceMinor, maxPriceMinor, inStock, true, sort, page, size, false);
    }

    @GetMapping("/products/facets")
    public FacetsResponse facets() { return products.facets(); }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() { return products.categories(); }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> find(@PathVariable UUID id) { return ResponseEntity.ok(products.findPublic(id)); }
}
