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

/**
 * Siembra el catalogo inicial cada vez que arranca la aplicacion.
 *
 * Solo se ejecuta cuando la tabla de productos esta vacia (count() == 0),
 * para evitar duplicados si la JVM sigue viva y permitir que el admin
 * agregue productos custom sin que se sobrescriban en caliente.
 */
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
            System.out.println("[DataSeeder] Productos ya existen, se omite el seeding.");
            return;
        }

        System.out.println("[DataSeeder] Iniciando siembra de datos...");

        Category ropa = ensureCategory("Ropa", null, 1);
        Category ropaHombre = ensureCategory("Hombre", ropa, 2);
        Category ropaMujer = ensureCategory("Mujer", ropa, 2);
        Category camisas = ensureCategory("Camisas", ropaHombre, 3);
        Category pantalones = ensureCategory("Pantalones", ropaHombre, 3);
        Category vestidos = ensureCategory("Vestidos", ropaMujer, 3);

        Category electronica = ensureCategory("Electronica", null, 1);
        Category audio = ensureCategory("Audio", electronica, 2);
        Category auriculares = ensureCategory("Auriculares", audio, 3);
        Category smartphones = ensureCategory("Smartphones", electronica, 2);

        Category hogar = ensureCategory("Hogar", null, 1);
        Category cocina = ensureCategory("Cocina", hogar, 2);
        Category decoracion = ensureCategory("Decoracion", hogar, 2);

        Category deportes = ensureCategory("Deportes", null, 1);
        Category fitness = ensureCategory("Fitness", deportes, 2);

        safeSeed(new SeedProduct(
                "Camisa Slim Fit Blanca",
                "Camisa de algodon slim fit, ideal para oficina.",
                4500, 3500, 22, 30, "ShopWave", "Blanco",
                camisas,
                "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=800",
                new HashSet<Size>()
        ));
        safeSeed(new SeedProduct(
                "Pantalon Jeans Clasico",
                "Jeans de corte recto, 100% denim.",
                7800, 5900, 24, 25, "ShopWave", "Azul",
                pantalones,
                "https://images.unsplash.com/photo-1542272604-787c3835535d?w=800",
                new HashSet<Size>()
        ));
        safeSeed(new SeedProduct(
                "Vestido Floral Verano",
                "Vestido fresco con estampado floral, perfecto para el verano.",
                6200, 4900, 21, 20, "ShopWave", "Multicolor",
                vestidos,
                "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=800",
                new HashSet<Size>()
        ));
        safeSeed(new SeedProduct(
                "Auriculares Bluetooth Pro",
                "Auriculares inalambricos con cancelacion de ruido y 30h de bateria.",
                12500, 8990, 28, 40, "AudioWave", "Negro",
                auriculares,
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800",
                new HashSet<Size>()
        ));
        safeSeed(new SeedProduct(
                "Smartphone Galaxy X",
                "Smartphone de ultima generacion con camara triple de 108MP.",
                45000, 38990, 13, 15, "TechMobile", "Negro",
                smartphones,
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800",
                new HashSet<Size>()
        ));
        safeSeed(new SeedProduct(
                "Set de Sartenes Antiadherentes",
                "Juego de 3 sartenes con recubrimiento antiadherente de granito.",
                8900, 5990, 33, 18, "HomeChef", "Negro",
                cocina,
                "https://images.unsplash.com/photo-1584990347449-a8d4d24a8d54?w=800",
                new HashSet<Size>()
        ));
        safeSeed(new SeedProduct(
                "Lampara de Mesa LED",
                "Lampara LED moderna con control tactil y 3 niveles de brillo.",
                4500, 3200, 29, 22, "Lumio", "Blanco",
                decoracion,
                "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800",
                new HashSet<Size>()
        ));
        safeSeed(new SeedProduct(
                "Mancuernas Ajustables 20kg",
                "Par de mancuernas ajustables de 2 a 20kg, ideales para entrenar en casa.",
                15000, 11990, 20, 12, "FitPro", "Negro",
                fitness,
                "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800",
                new HashSet<Size>()
        ));
        safeSeed(new SeedProduct(
                "Camiseta Deportiva Hombre",
                "Camiseta tecnica transpirable para running.",
                3200, 2290, 28, 50, "SportLine", "Gris",
                fitness,
                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800",
                new HashSet<Size>()
        ));
        safeSeed(new SeedProduct(
                "Mochila Urbana Antirrobo",
                "Mochila con compartimento para laptop de 15.6 pulgadas y diseno antirrobo.",
                6800, 4590, 32, 28, "UrbanPack", "Negro",
                fitness,
                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800",
                new HashSet<Size>()
        ));

        System.out.println("[DataSeeder] Siembra completada. Productos: " + productRepository.count());
    }

    private Category ensureCategory(String name, Category parent, int level) {
        try {
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
        } catch (Exception e) {
            System.err.println("[DataSeeder] Error creando categoria " + name + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Crea un Set&lt;Size&gt; a partir de pares nombre/cantidad.
     * Util para declarar tallas de forma concisa al construir el catalogo.
     */
    private static Set<Size> t(String... nameQtyPairs) {
        if (nameQtyPairs == null || nameQtyPairs.length == 0 || nameQtyPairs.length % 2 != 0) {
            return new HashSet<>();
        }
        Set<Size> result = new HashSet<>();
        for (int i = 0; i < nameQtyPairs.length; i += 2) {
            String name = nameQtyPairs[i];
            int qty;
            try {
                qty = Integer.parseInt(nameQtyPairs[i + 1]);
            } catch (NumberFormatException ex) {
                qty = 0;
            }
            Size s = new Size();
            s.setName(name);
            s.setQuantity(qty);
            result.add(s);
        }
        return result;
    }

    private void safeSeed(SeedProduct seed) {
        try {
            if (seed.category == null) {
                System.err.println("[DataSeeder] Categoria nula, omitiendo producto: " + seed.title);
                return;
            }
            int sizesTotal = seed.sizes.stream().mapToInt(Size::getQuantity).sum();
            if (sizesTotal != seed.quantity) {
                System.err.println("[DataSeeder] Mismatch stock/tallas en '" + seed.title
                        + "': stock=" + seed.quantity + " sizes=" + sizesTotal
                        + " (se ajusta el stock para mantener la invariante).");
                seed.quantity = sizesTotal;
            }

            Product p = new Product();
            p.setTitle(seed.title);
            p.setDescription(seed.description);
            p.setPrice(seed.price);
            p.setDiscountedPrice(seed.discountedPrice);
            p.setDiscountPersent(seed.discountPersent);
            p.setQuantity(seed.quantity);
            p.setBrand(seed.brand);
            p.setColor(seed.color);
            p.setCategory(seed.category);
            p.setSizes(new HashSet<>(seed.sizes));
            p.setImageUrl(seed.imageUrl);
            p.setNumRatings(0);
            p.setCreatedAt(LocalDateTime.now());
            productRepository.save(p);
            System.out.println("[DataSeeder] Producto creado: " + seed.title
                    + " (stock=" + seed.quantity + ", tallas=" + seed.sizes.size() + ")");
        } catch (Exception e) {
            System.err.println("[DataSeeder] Error creando producto " + seed.title
                    + ": " + e.getMessage());
        }
    }

    /**
     * DTO liviano para declarar productos del catalogo inicial.
     * Centraliza todos los campos necesarios para sembrar un Product
     * y permite construir catalogos grandes sin perder legibilidad.
     */
    private static final class SeedProduct {
        final String title;
        final String description;
        final int price;
        final int discountedPrice;
        final int discountPersent;
        int quantity;
        final String brand;
        final String color;
        final Category category;
        final String imageUrl;
        final Set<Size> sizes;

        SeedProduct(String title, String description, int price, int discountedPrice,
                    int discountPersent, int quantity, String brand, String color,
                    Category category, String imageUrl, Set<Size> sizes) {
            this.title = title;
            this.description = description;
            this.price = price;
            this.discountedPrice = discountedPrice;
            this.discountPersent = discountPersent;
            this.quantity = quantity;
            this.brand = brand;
            this.color = color;
            this.category = category;
            this.imageUrl = imageUrl;
            this.sizes = sizes;
        }
    }
}
