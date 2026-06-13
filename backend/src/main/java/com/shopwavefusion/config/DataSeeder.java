package com.shopwavefusion.config;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.shopwavefusion.modal.Category;
import com.shopwavefusion.modal.Product;
import com.shopwavefusion.modal.Size;
import com.shopwavefusion.repository.CategoryRepository;
import com.shopwavefusion.repository.ProductRepository;

@Component
@Order(2)
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public DataSeeder(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() > 0) {
            return;
        }

        Category ropa = ensureCategory("Ropa", null, 1);
        Category ropaHombre = ensureCategory("Hombre", ropa, 2);
        Category ropaMujer = ensureCategory("Mujer", ropa, 2);
        Category camisas = ensureCategory("Camisas", ropaHombre, 3);
        Category pantalones = ensureCategory("Pantalones", ropaHombre, 3);
        Category vestidos = ensureCategory("Vestidos", ropaMujer, 3);

        Category electronica = ensureCategory("Electrónica", null, 1);
        Category audio = ensureCategory("Audio", electronica, 2);
        Category auriculares = ensureCategory("Auriculares", audio, 3);
        Category smartphones = ensureCategory("Smartphones", electronica, 2);

        Category hogar = ensureCategory("Hogar", null, 1);
        Category cocina = ensureCategory("Cocina", hogar, 2);
        Category decoracion = ensureCategory("Decoración", hogar, 2);

        Category deportes = ensureCategory("Deportes", null, 1);
        Category fitness = ensureCategory("Fitness", deportes, 2);

        seed("Camisa Slim Fit Blanca", "Camisa de algodón slim fit, ideal para oficina.", 4500, 3500, 22, 30, "ShopWave", "Blanco", camisas, sizes(30, "S", "M", "L", "XL"), "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=800");
        seed("Pantalón Jeans Clásico", "Jeans de corte recto, 100% denim.", 7800, 5900, 24, 25, "ShopWave", "Azul", pantalones, sizes(25, "30", "32", "34", "36"), "https://images.unsplash.com/photo-1542272604-787c3835535d?w=800");
        seed("Vestido Floral Verano", "Vestido fresco con estampado floral, perfecto para el verano.", 6200, 4900, 21, 20, "ShopWave", "Multicolor", vestidos, sizes(20, "S", "M", "L"), "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=800");
        seed("Auriculares Bluetooth Pro", "Auriculares inalámbricos con cancelación de ruido y 30h de batería.", 12500, 8990, 28, 40, "AudioWave", "Negro", auriculares, sizes(40), "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800");
        seed("Smartphone Galaxy X", "Smartphone de última generación con cámara triple de 108MP.", 45000, 38990, 13, 15, "TechMobile", "Negro", smartphones, sizes(15), "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800");
        seed("Set de Sartenes Antiadherentes", "Juego de 3 sartenes con recubrimiento antiadherente de granito.", 8900, 5990, 33, 18, "HomeChef", "Negro", cocina, sizes(18), "https://images.unsplash.com/photo-1584990347449-a8d4d24a8d54?w=800");
        seed("Lámpara de Mesa LED", "Lámpara LED moderna con control táctil y 3 niveles de brillo.", 4500, 3200, 29, 22, "Lumio", "Blanco", decoracion, sizes(22), "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800");
        seed("Mancuernas Ajustables 20kg", "Par de mancuernas ajustables de 2 a 20kg, ideales para entrenar en casa.", 15000, 11990, 20, 12, "FitPro", "Negro", fitness, sizes(12), "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800");
        seed("Camiseta Deportiva Hombre", "Camiseta técnica transpirable para running.", 3200, 2290, 28, 50, "SportLine", "Gris", fitness, sizes(50, "S", "M", "L", "XL"), "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800");
        seed("Mochila Urbana Antirrobo", "Mochila con compartimento para laptop de 15.6 pulgadas y diseño antirrobo.", 6800, 4590, 32, 28, "UrbanPack", "Negro", fitness, sizes(28), "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800");
    }

    private Category ensureCategory(String name, Category parent, int level) {
        Category existing;
        if (parent == null) {
            existing = categoryRepository.findByName(name);
        } else {
            existing = categoryRepository.findByNameAndParant(name, parent.getName());
        }
        if (existing != null) {
            return existing;
        }
        Category c = new Category();
        c.setName(name);
        c.setParentCategory(parent);
        c.setLevel(level);
        return categoryRepository.save(c);
    }

    private void seed(String title, String description, int price, int discountedPrice,
                      int discountPersent, int quantity, String brand, String color,
                      Category category, Set<Size> sizes, String imageUrl) {
        Product p = new Product();
        p.setTitle(title);
        p.setDescription(description);
        p.setPrice(price);
        p.setDiscountedPrice(discountedPrice);
        p.setDiscountPersent(discountPersent);
        p.setQuantity(quantity);
        p.setBrand(brand);
        p.setColor(color);
        p.setCategory(category);
        p.setSizes(sizes);
        p.setImageUrl(imageUrl);
        p.setNumRatings(0);
        p.setCreatedAt(LocalDateTime.now());
        productRepository.save(p);
    }

    private Set<Size> sizes(int defaultQty, String... names) {
        Set<Size> result = new HashSet<>();
        for (String name : names) {
            Size s = new Size();
            s.setName(name);
            s.setQuantity(defaultQty);
            result.add(s);
        }
        return result;
    }

    private Set<Size> sizes(int qty) {
        return sizes(qty, "Unico");
    }
}
