package com.shopwavefusion.rework.service;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopwavefusion.rework.api.v1.ApiException;
import com.shopwavefusion.rework.api.v1.CommonDtos;
import com.shopwavefusion.rework.api.v1.ProductDtos.ProductWriteRequest;
import com.shopwavefusion.rework.api.v1.ProductDtos.VariantWriteRequest;
import com.shopwavefusion.rework.api.v1.ProductMapper;
import com.shopwavefusion.rework.domain.CategoryEntity;
import com.shopwavefusion.rework.domain.ProductEntity;
import com.shopwavefusion.rework.domain.ProductVariantEntity;
import com.shopwavefusion.rework.repository.CategoryRepository;
import com.shopwavefusion.rework.repository.CartItemRepository;
import com.shopwavefusion.rework.repository.ProductRepository;
import com.shopwavefusion.rework.repository.ProductSpecifications;
import com.shopwavefusion.rework.repository.ProductVariantRepository;

import static com.shopwavefusion.rework.api.v1.CommonDtos.*;

@Service
public class ProductService {
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final CategoryRepository categories;
    private final CartItemRepository cartItems;
    private final ProductMapper mapper;

    public ProductService(ProductRepository products, ProductVariantRepository variants,
                          CategoryRepository categories, CartItemRepository cartItems, ProductMapper mapper) {
        this.products = products;
        this.variants = variants;
        this.categories = categories;
        this.cartItems = cartItems;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(String q, UUID categoryId, List<String> colors,
                                               List<String> labels, Long minPrice, Long maxPrice,
                                               Boolean inStock, Boolean active, String sort, int page, int size, boolean admin) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(size, 1), 48);
        Specification<ProductEntity> spec = Specification.where(ProductSpecifications.active(admin ? active : true))
                .and(ProductSpecifications.text(q))
                .and(ProductSpecifications.categoryIn(categoryId == null ? null : descendants(categoryId)))
                .and(ProductSpecifications.colors(colors))
                .and(ProductSpecifications.variants(labels))
                .and(ProductSpecifications.priceBetween(minPrice, maxPrice))
                .and(ProductSpecifications.inStock(inStock));
        Pageable pageable = PageRequest.of(safePage, safeSize, sortFor(sort));
        Page<ProductEntity> result = products.findAll(spec, pageable);
        return new PageResponse<>(result.getContent().stream().map(mapper::product).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProductResponse findPublic(UUID id) {
        return mapper.product(products.findByIdAndActiveTrue(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found")));
    }

    @Transactional(readOnly = true)
    public ProductResponse findAdmin(UUID id) { return mapper.product(findEntity(id)); }

    @Transactional(readOnly = true)
    public ProductEntity findEntity(UUID id) {
        return products.findById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found"));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> categories() {
        return categories.findAllByOrderByLevelAscNameAsc().stream().map(mapper::category).toList();
    }

    @Transactional(readOnly = true)
    public FacetsResponse facets() {
        List<ProductEntity> all = products.findAllByActiveTrue(Pageable.unpaged()).getContent();
        Set<String> colors = new HashSet<>();
        Set<String> labels = new HashSet<>();
        long min = Long.MAX_VALUE;
        long max = 0;
        for (ProductEntity product : all) {
            colors.add(product.getColor());
            product.getVariants().stream().filter(ProductVariantEntity::isActive).map(ProductVariantEntity::getLabel)
                    .forEach(labels::add);
            min = Math.min(min, product.getSalePriceMinor());
            max = Math.max(max, product.getSalePriceMinor());
        }
        return new FacetsResponse(all.stream().map(ProductEntity::getCategory).distinct().map(mapper::category).toList(),
                colors.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                labels.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), min == Long.MAX_VALUE ? 0 : min, max);
    }

    @Transactional
    public ProductResponse create(ProductWriteRequest request) {
        validateWrite(request);
        ProductEntity product = new ProductEntity();
        product.setCreatedAt(Instant.now());
        apply(product, request, true);
        return mapper.product(products.saveAndFlush(product));
    }

    @Transactional
    public ProductResponse update(UUID id, ProductWriteRequest request) {
        validateWrite(request);
        ProductEntity product = findEntity(id);
        if (request.version() == null || request.version() != product.getVersion()) {
            throw new ApiException(HttpStatus.CONFLICT, "STALE_PRODUCT", "Product changed; reload before saving");
        }
        apply(product, request, false);
        return mapper.product(products.saveAndFlush(product));
    }

    @Transactional
    public ProductResponse archive(UUID id, boolean active, long version) {
        ProductEntity product = findEntity(id);
        if (product.getVersion() != version) {
            throw new ApiException(HttpStatus.CONFLICT, "STALE_PRODUCT", "Product changed; reload before saving");
        }
        product.setActive(active);
        product.setUpdatedAt(Instant.now());
        return mapper.product(products.saveAndFlush(product));
    }

    private void validateWrite(ProductWriteRequest request) {
        if (request.priceMinor() < request.salePriceMinor()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PRICE", "Sale price cannot exceed price");
        }
        if (!isAllowedImage(request.imageUrl())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_URL", "Image URL must be https or a local path");
        }
        Set<String> unique = new HashSet<>();
        for (VariantWriteRequest variant : request.variants()) {
            if (variant.stock() < 0 || !unique.add(normalize(variant.label()))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_VARIANTS", "Variant labels must be unique and stock non-negative");
            }
        }
    }

    private void apply(ProductEntity product, ProductWriteRequest request, boolean create) {
        if (request.categoryId() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_REQUIRED", "Category is required");
        CategoryEntity category = categories.findById(request.categoryId()).orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_NOT_FOUND", "Category does not exist"));
        product.setTitle(request.title().trim());
        product.setDescription(request.description().trim());
        product.setBrand(request.brand().trim());
        product.setColor(request.color().trim());
        product.setImageUrl(request.imageUrl().trim());
        product.setPriceMinor(request.priceMinor());
        product.setSalePriceMinor(request.salePriceMinor());
        product.setDiscountPercent((int) Math.round((1d - (double) request.salePriceMinor() / request.priceMinor()) * 100));
        product.setCategory(category);
        product.setActive(request.active() == null || request.active());
        product.setUpdatedAt(Instant.now());
        Map<UUID, ProductVariantEntity> existing = new HashMap<>();
        product.getVariants().forEach(v -> existing.put(v.getId(), v));
        Set<UUID> seen = new HashSet<>();
        for (VariantWriteRequest input : request.variants()) {
            ProductVariantEntity variant = input.id() == null ? new ProductVariantEntity() : existing.get(input.id());
            if (variant == null || (!create && !seen.add(input.id()))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_VARIANT", "Variant does not belong to product");
            }
            if (!create && input.id() != null && !normalize(variant.getLabel()).equals(normalize(input.label()))
                    && cartItems.existsByVariantId(input.id())) {
                throw new ApiException(HttpStatus.CONFLICT, "VARIANT_LABEL_IN_USE", "A variant in a cart cannot be renamed");
            }
            variant.setProduct(product);
            variant.setLabel(input.label().trim());
            variant.setLabelNormalized(normalize(input.label()));
            variant.setStock(input.stock());
            variant.setActive(input.active());
            if (!product.getVariants().contains(variant)) product.getVariants().add(variant);
        }
        if (!create && existing.keySet().stream().anyMatch(id -> !seen.contains(id))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_VARIANT", "Existing variants must be archived explicitly");
        }
    }

    private Set<UUID> descendants(UUID root) {
        List<CategoryEntity> all = categories.findAll();
        Set<UUID> ids = new HashSet<>();
        ids.add(root);
        boolean changed;
        do {
            changed = false;
            for (CategoryEntity category : all) {
                if (category.getParent() != null && ids.contains(category.getParent().getId()) && ids.add(category.getId())) changed = true;
            }
        } while (changed);
        return ids;
    }

    private Sort sortFor(String sort) {
        return switch (sort == null ? "newest" : sort) {
            case "price_asc" -> Sort.by(Sort.Order.asc("salePriceMinor"), Sort.Order.asc("id"));
            case "price_desc" -> Sort.by(Sort.Order.desc("salePriceMinor"), Sort.Order.asc("id"));
            case "discount_desc" -> Sort.by(Sort.Order.desc("discountPercent"), Sort.Order.asc("id"));
            default -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));
        };
    }

    private static boolean isAllowedImage(String value) {
        if (value == null || value.isBlank()) return false;
        if (value.startsWith("/")) return !value.startsWith("//");
        try { return "https".equalsIgnoreCase(URI.create(value).getScheme()); }
        catch (IllegalArgumentException ex) { return false; }
    }

    public static String normalize(String value) { return value.trim().toLowerCase(Locale.ROOT); }
}
