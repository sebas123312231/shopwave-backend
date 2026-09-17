package com.shopwavefusion.rework.api.v1;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.shopwavefusion.rework.domain.CategoryEntity;
import com.shopwavefusion.rework.domain.ProductEntity;
import com.shopwavefusion.rework.domain.ProductVariantEntity;

import static com.shopwavefusion.rework.api.v1.CommonDtos.*;

@Component
public class ProductMapper {
    public ProductResponse product(ProductEntity product) {
        List<VariantResponse> variants = product.getVariants().stream()
                .map(v -> new VariantResponse(v.getId(), v.getLabel(), v.getStock(), v.isActive()))
                .toList();
        int stock = variants.stream().filter(VariantResponse::active).mapToInt(VariantResponse::stock).sum();
        return new ProductResponse(product.getId(), product.getTitle(), product.getDescription(), product.getBrand(),
                product.getColor(), product.getImageUrl(), product.getPriceMinor(), product.getSalePriceMinor(), "BOB",
                product.getDiscountPercent(), stock, variants, category(product.getCategory()), product.isActive(),
                product.getVersion(), product.getCreatedAt(), product.getUpdatedAt());
    }

    public CategoryResponse category(CategoryEntity category) {
        List<CategoryPathItem> path = new ArrayList<>();
        CategoryEntity current = category;
        int guard = 0;
        while (current != null && guard++ < 4) {
            path.add(0, new CategoryPathItem(current.getId(), current.getName()));
            current = current.getParent();
        }
        return new CategoryResponse(category.getId(), category.getName(),
                category.getParent() == null ? null : category.getParent().getId(), path);
    }
}
