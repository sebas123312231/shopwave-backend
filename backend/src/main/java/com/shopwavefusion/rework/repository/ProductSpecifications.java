package com.shopwavefusion.rework.repository;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.shopwavefusion.rework.domain.ProductEntity;
import com.shopwavefusion.rework.domain.ProductVariantEntity;

import jakarta.persistence.criteria.JoinType;

public final class ProductSpecifications {
    private ProductSpecifications() {}

    public static Specification<ProductEntity> active(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active);
    }
    public static Specification<ProductEntity> text(String queryText) {
        if (queryText == null || queryText.isBlank()) return null;
        String like = "%" + queryText.trim().toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), like, '\\'),
                cb.like(cb.lower(root.get("brand")), like, '\\'),
                cb.like(cb.lower(root.get("description")), like, '\\'));
    }
    public static Specification<ProductEntity> categoryIn(Collection<UUID> ids) {
        return ids == null || ids.isEmpty() ? null : (root, query, cb) -> root.get("category").get("id").in(ids);
    }
    public static Specification<ProductEntity> colors(Collection<String> colors) {
        return colors == null || colors.isEmpty() ? null : (root, query, cb) ->
                cb.lower(root.get("color")).in(colors.stream().map(v -> v.toLowerCase(Locale.ROOT)).toList());
    }
    public static Specification<ProductEntity> variants(Collection<String> labels) {
        return labels == null || labels.isEmpty() ? null : (root, query, cb) -> {
            query.distinct(true);
            var join = root.join("variants", JoinType.INNER);
            return join.get("labelNormalized").in(labels.stream().map(v -> v.trim().toLowerCase(Locale.ROOT)).toList());
        };
    }
    public static Specification<ProductEntity> priceBetween(Long min, Long max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get("salePriceMinor"), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get("salePriceMinor"), min);
            return cb.between(root.get("salePriceMinor"), min, max);
        };
    }
    public static Specification<ProductEntity> inStock(Boolean inStock) {
        return inStock == null ? null : (root, query, cb) -> {
            var subquery = query.subquery(UUID.class);
            var variant = subquery.from(ProductVariantEntity.class);
            subquery.select(variant.get("id"));
            subquery.where(cb.equal(variant.get("product").get("id"), root.get("id")),
                    cb.isTrue(variant.get("active")), cb.greaterThan(variant.get("stock"), 0));
            return inStock ? cb.exists(subquery) : cb.not(cb.exists(subquery));
        };
    }
}
