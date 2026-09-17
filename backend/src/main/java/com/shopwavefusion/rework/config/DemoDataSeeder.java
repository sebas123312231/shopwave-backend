package com.shopwavefusion.rework.config;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.shopwavefusion.rework.domain.CategoryEntity;
import com.shopwavefusion.rework.domain.ProductEntity;
import com.shopwavefusion.rework.domain.ProductVariantEntity;
import com.shopwavefusion.rework.repository.CategoryRepository;
import com.shopwavefusion.rework.repository.ProductRepository;

@Component
@ConditionalOnProperty(name = "shopwave.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {
    private final ProductRepository products;
    private final CategoryRepository categories;

    public DemoDataSeeder(ProductRepository products, CategoryRepository categories) { this.products = products; this.categories = categories; }

    @Override
    @Transactional
    public void run(String... args) {
        if (products.count() > 0) return;
        List<CategoryEntity> roots = new ArrayList<>();
        for (String name : List.of("Ropa", "Tecnología", "Hogar")) {
            CategoryEntity category = new CategoryEntity(); category.setName(name); category.setSlug(slug(name)); category.setLevel(1); roots.add(categories.save(category));
        }
        Instant now = Instant.now();
        for (int i = 0; i < 30; i++) {
            CategoryEntity category = roots.get(i % roots.size());
            ProductEntity product = new ProductEntity(); product.setTitle("ShopWave " + (i + 1)); product.setDescription("Producto de demostración curado para el catálogo local de ShopWave."); product.setBrand(i % 2 == 0 ? "ShopWave" : "Studio"); product.setColor(List.of("Negro", "Blanco", "Azul", "Verde").get(i % 4)); product.setImageUrl("https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800"); product.setPriceMinor(1900L + i * 125); product.setSalePriceMinor(1500L + i * 100); product.setDiscountPercent(20); product.setCategory(category); product.setCreatedAt(now); product.setUpdatedAt(now);
            for (String label : List.of("S", "M", "L")) { ProductVariantEntity variant = new ProductVariantEntity(); variant.setProduct(product); variant.setLabel(label); variant.setLabelNormalized(slug(label)); variant.setStock(5 + (i % 6)); variant.setActive(true); product.getVariants().add(variant); }
            products.save(product);
        }
    }

    private static String slug(String value) { return value.trim().toLowerCase(Locale.ROOT).replace(' ', '-'); }
}
