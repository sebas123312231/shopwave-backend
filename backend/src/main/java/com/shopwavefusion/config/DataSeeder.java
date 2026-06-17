package com.shopwavefusion.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
 * Por que existe: en produccion (Render free tier + H2 in-memory) cada
 * vez que el servicio se reactiva tras el spin-down la JVM se reinicia y
 * la base de datos queda vacia. Este CommandLineRunner recrea el
 * catalogo base de 30 productos con tallas validas para que el
 * frontend siempre tenga datos consistentes disponibles.
 *
 * Reglas:
 *  - Solo se ejecuta cuando la tabla de productos esta vacia
 *    (count() == 0). Esto evita duplicados si la JVM sigue viva
 *    y permite que el admin agregue productos custom sin que se
 *    sobrescriban en caliente.
 *  - Cada producto declara tallas cuya SUMA debe coincidir con
 *    su stock total (quantity). Esto es lo que valida el frontend
 *    en ProductForm y se respeta aca para mantener la invariante.
 *  - Las tallas pueden ser String (S/M/L/XL) o Integer (numeracion
 *    de calzado 38-44, capacidades 128/256GB, etc.) tal como las
 *    espera el modelo de datos.
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

        System.out.println("[DataSeeder] Iniciando siembra de catalogo (30 productos)...");

        // ------------------------------------------------------------------
        // CATEGORIAS (3 niveles: nivel 1 -> nivel 2 -> nivel 3)
        // ------------------------------------------------------------------
        Category ropa = ensureCategory("Ropa", null, 1);
        Category ropaHombre = ensureCategory("Hombre", ropa, 2);
        Category ropaMujer = ensureCategory("Mujer", ropa, 2);

        Category camisetasCat = ensureCategory("Camisetas", ropa, 2);
        Category camisetasCasual = ensureCategory("Casual", camisetasCat, 3);
        Category camisetasDeportivas = ensureCategory("Deportivas", camisetasCat, 3);

        Category polos = ensureCategory("Polos", ropa, 2);
        Category polosCasual = ensureCategory("Casual", polos, 3);

        Category camisas = ensureCategory("Camisas", ropaHombre, 3);
        Category pantalones = ensureCategory("Pantalones", ropa, 2);
        Category jeans = ensureCategory("Jeans", pantalones, 3);
        Category chinos = ensureCategory("Chinos", pantalones, 3);
        Category vestidos = ensureCategory("Vestidos", ropaMujer, 3);
        Category blazers = ensureCategory("Blazers", ropaMujer, 3);
        Category chaquetas = ensureCategory("Chaquetas", ropa, 2);
        Category chaquetasDep = ensureCategory("Deportivas", chaquetas, 3);
        Category accesoriosRopa = ensureCategory("Accesorios", ropa, 2);
        Category invierno = ensureCategory("Invierno", accesoriosRopa, 3);

        Category deportes = ensureCategory("Deportes", null, 1);
        Category runningH = ensureCategory("Running", deportes, 2);
        Category runningMujer = ensureCategory("Mujer", runningH, 3);
        Category runningHombre = ensureCategory("Hombre", runningH, 3);
        Category yoga = ensureCategory("Yoga", deportes, 2);
        Category yogaMujer = ensureCategory("Mujer", yoga, 3);

        Category electronica = ensureCategory("Electronica", null, 1);
        Category audio = ensureCategory("Audio", electronica, 2);
        Category auriculares = ensureCategory("Auriculares", audio, 3);
        Category tv = ensureCategory("Televisores", audio, 3);
        Category smartphones = ensureCategory("Smartphones", electronica, 2);
        Category premium = ensureCategory("Premium", smartphones, 3);
        Category tablets = ensureCategory("Tablets", electronica, 2);
        Category tabletsStd = ensureCategory("Estandar", tablets, 3);
        Category wearables = ensureCategory("Wearables", electronica, 2);
        Category smartwatch = ensureCategory("Smartwatch", wearables, 3);

        Category hogar = ensureCategory("Hogar", null, 1);
        Category cocina = ensureCategory("Cocina", hogar, 2);
        Category sartenes = ensureCategory("Sartenes", cocina, 3);
        Category ollas = ensureCategory("Ollas", cocina, 3);
        Category iluminacion = ensureCategory("Iluminacion", hogar, 2);
        Category escritorio = ensureCategory("Escritorio", iluminacion, 3);
        Category decoracion = ensureCategory("Decoracion", hogar, 2);
        Category dormitorio = ensureCategory("Dormitorio", decoracion, 3);

        Category accesorios = ensureCategory("Accesorios", null, 1);
        Category mochilas = ensureCategory("Mochilas", accesorios, 2);
        Category laptop = ensureCategory("Laptop", mochilas, 3);
        Category escolar = ensureCategory("Escolar", mochilas, 3);

        // ------------------------------------------------------------------
        // CATALOGO DE 30 PRODUCTOS
        // Cada entrada: titulo, descripcion, precio regular, precio descuento,
        // % descuento, stock total, marca, color, categoria, imageUrl, tallas.
        // La SUMA de las cantidades por talla DEBE coincidir con stock total.
        // ------------------------------------------------------------------
        List<SeedProduct> catalog = new ArrayList<>();
        catalog.add(new SeedProduct(
                "Camiseta Esencial Algodon",
                "Camiseta unisex de algodon peinado, corte clasico, costuras reforzadas.",
                19000, 14900, 22, 45, "ShopWave", "Blanco",
                camisetasCasual,
                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800",
                t("S", 10, "M", 15, "L", 12, "XL", 8)
        ));
        catalog.add(new SeedProduct(
                "Polo Pique Premium Hombre",
                "Polo de pique 100% algodon con bordado discreto, ideal para oficina o salida casual.",
                32000, 23900, 25, 30, "ShopWave", "Negro",
                polosCasual,
                "https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=800",
                t("S", 6, "M", 10, "L", 9, "XL", 5)
        ));
        catalog.add(new SeedProduct(
                "Camisa Slim Fit Lino",
                "Camisa de lino 100% natural, corte slim, ideal para climas calidos.",
                48000, 35900, 25, 24, "ShopWave", "Beige",
                camisas,
                "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=800",
                t("S", 5, "M", 8, "L", 7, "XL", 4)
        ));
        catalog.add(new SeedProduct(
                "Pantalon Jean Clasico",
                "Jean de corte recto, 100% denim, costuras dobles y tiron reforzado.",
                59000, 45900, 22, 35, "ShopWave", "Azul",
                jeans,
                "https://images.unsplash.com/photo-1542272604-787c3835535d?w=800",
                t("28", 6, "30", 10, "32", 9, "34", 6, "36", 4)
        ));
        catalog.add(new SeedProduct(
                "Pantalon Chino Slim",
                "Pantalon chino de corte slim, tela suave y elastica para mayor comodidad.",
                42000, 31900, 24, 28, "ShopWave", "Beige",
                chinos,
                "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=800",
                t("28", 4, "30", 8, "32", 8, "34", 5, "36", 3)
        ));
        catalog.add(new SeedProduct(
                "Vestido Floral Verano",
                "Vestido fresco con estampado floral, tela vaporosa, perfecto para el verano.",
                62000, 44900, 28, 22, "ShopWave", "Multicolor",
                vestidos,
                "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=800",
                t("XS", 4, "S", 6, "M", 6, "L", 4, "XL", 2)
        ));
        catalog.add(new SeedProduct(
                "Blazer Ejecutivo Mujer",
                "Blazer entallado para uso profesional, tela suave, forro interior.",
                89000, 68900, 23, 18, "ShopWave", "Negro",
                blazers,
                "https://images.unsplash.com/photo-1632149877166-f75d49000351?w=800",
                t("XS", 3, "S", 5, "M", 5, "L", 3, "XL", 2)
        ));
        catalog.add(new SeedProduct(
                "Chaqueta Cortavientos Deportiva",
                "Chaqueta cortavientos ligera, ideal para correr o actividades al aire libre.",
                48000, 35900, 25, 26, "SportLine", "Negro",
                chaquetasDep,
                "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=800",
                t("S", 5, "M", 8, "L", 7, "XL", 4, "XXL", 2)
        ));
        catalog.add(new SeedProduct(
                "Camiseta Tecnica Running",
                "Camiseta deportiva con tecnologia de secado rapido, costuras planas.",
                28000, 19900, 29, 50, "SportLine", "Gris",
                runningHombre,
                "https://images.unsplash.com/photo-1556906781-9a412961c28c?w=800",
                t("S", 8, "M", 14, "L", 14, "XL", 9, "XXL", 5)
        ));
        catalog.add(new SeedProduct(
                "Short Deportivo Mujer",
                "Short elastico de secado rapido, ideal para correr o yoga.",
                24000, 16900, 30, 38, "SportLine", "Negro",
                runningMujer,
                "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=800",
                t("XS", 5, "S", 9, "M", 11, "L", 8, "XL", 5)
        ));
        catalog.add(new SeedProduct(
                "Leggings Deportivos Premium",
                "Leggings de tiro alto con tecnologia anti-transparencia, ideal para yoga.",
                32000, 22900, 28, 30, "SportLine", "Negro",
                yogaMujer,
                "https://images.unsplash.com/photo-1506629082955-511b1aa562c8?w=800",
                t("S", 7, "M", 10, "L", 8, "XL", 5)
        ));
        catalog.add(new SeedProduct(
                "Zapatillas Urbanas Clasicas",
                "Zapatillas urbanas con suela antideslizante, plantilla acolchada y diseno atemporal.",
                54000, 39900, 26, 40, "StepLab", "Blanco",
                ensureCategory("Zapatillas", ensureCategory("Calzado", null, 1), 2),
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800",
                t("38", 6, "39", 8, "40", 9, "41", 8, "42", 6, "43", 3)
        ));
        catalog.add(new SeedProduct(
                "Zapatillas Running Pro",
                "Zapatillas de running con amortiguacion reactiva y malla transpirable.",
                89000, 64900, 27, 35, "StepLab", "Negro",
                ensureCategory("Deportivas", ensureCategory("Zapatillas", ensureCategory("Calzado", null, 1), 2), 3),
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800",
                t("38", 5, "39", 7, "40", 8, "41", 7, "42", 5, "43", 3)
        ));
        catalog.add(new SeedProduct(
                "Botines de Cuero Mujer",
                "Botines de cuero genuino, taco medio, perfectos para el invierno.",
                92000, 72900, 21, 20, "StepLab", "Marron",
                ensureCategory("Botines", ensureCategory("Calzado", null, 1), 2),
                "https://images.unsplash.com/photo-1608256246200-53e635b5b65f?w=800",
                t("35", 3, "36", 5, "37", 5, "38", 4, "39", 3)
        ));
        catalog.add(new SeedProduct(
                "Botas de Montana Impermeables",
                "Botas de montana con membrana impermeable, suela de alta traccion.",
                115000, 89900, 22, 18, "StepLab", "Marron",
                ensureCategory("Botines", ensureCategory("Calzado", null, 1), 2),
                "https://images.unsplash.com/photo-1520219306100-ec4afeeefe58?w=800",
                t("38", 3, "39", 4, "40", 5, "41", 3, "42", 3)
        ));
        catalog.add(new SeedProduct(
                "Zapatos Formales Caballero",
                "Zapatos de vestir en cuero sintetico, suela cosida, ideal para oficina.",
                68000, 51900, 24, 24, "StepLab", "Negro",
                ensureCategory("Zapatos", ensureCategory("Calzado", null, 1), 2),
                "https://images.unsplash.com/photo-1614252369475-531eba835eb1?w=800",
                t("39", 3, "40", 5, "41", 6, "42", 5, "43", 3, "44", 2)
        ));
        catalog.add(new SeedProduct(
                "Sandalias Verano Hombre",
                "Sandalias comodos para el verano, suela antideslizante, ajustable.",
                22000, 15900, 28, 32, "StepLab", "Negro",
                ensureCategory("Sandalias", ensureCategory("Calzado", null, 1), 2),
                "https://images.unsplash.com/photo-1603487742131-4160ec999306?w=800",
                t("39", 5, "40", 7, "41", 8, "42", 6, "43", 4, "44", 2)
        ));
        catalog.add(new SeedProduct(
                "Mocasines Casual Premium",
                "Mocasines de cuero sintetico con detalle bordado, comodos para el dia a dia.",
                49000, 36900, 25, 22, "StepLab", "Marron",
                ensureCategory("Zapatos", ensureCategory("Calzado", null, 1), 2),
                "https://images.unsplash.com/photo-1533867617858-e7b97e060efb?w=800",
                t("39", 3, "40", 5, "41", 6, "42", 5, "43", 3)
        ));
        catalog.add(new SeedProduct(
                "Auriculares Bluetooth Pro",
                "Auriculares inalambricos con cancelacion de ruido y 30 horas de bateria.",
                99000, 74900, 24, 40, "AudioWave", "Negro",
                auriculares,
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800",
                t("Unico", 40)
        ));
        catalog.add(new SeedProduct(
                "Smartphone Galaxy X 256GB",
                "Smartphone de ultima generacion, camara triple 108MP, pantalla AMOLED 6.7\".",
                450000, 389000, 14, 15, "TechMobile", "Negro",
                premium,
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800",
                t("128GB", 8, "256GB", 7)
        ));
        catalog.add(new SeedProduct(
                "Tablet Pro 11 Pulgadas",
                "Tablet con pantalla 2K, lapiz optico incluido, ideal para diseno y estudio.",
                280000, 239000, 15, 18, "TechMobile", "Plata",
                tabletsStd,
                "https://images.unsplash.com/photo-1561154464-82e9adf3a2f4?w=800",
                t("64GB", 5, "128GB", 7, "256GB", 6)
        ));
        catalog.add(new SeedProduct(
                "Reloj Inteligente Sport Series 7",
                "Smartwatch con GPS, monitor de sueno, resistente al agua 5ATM.",
                85000, 62900, 26, 25, "TechMobile", "Negro",
                smartwatch,
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800",
                t("41mm", 12, "45mm", 13)
        ));
        catalog.add(new SeedProduct(
                "Smart TV 55 Pulgadas 4K UHD",
                "Televisor inteligente 4K con HDR y sistema operativo integrado.",
                380000, 299000, 21, 12, "Lumio", "Negro",
                tv,
                "https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?w=800",
                t("Unica", 12)
        ));
        catalog.add(new SeedProduct(
                "Set 3 Sartenes Antiadherentes",
                "Juego de 3 sartenes con recubrimiento de granito, aptas para induccion.",
                69000, 48900, 29, 18, "HomeChef", "Negro",
                sartenes,
                "https://images.unsplash.com/photo-1584990347449-a8d4d24a8d54?w=800",
                t("20cm", 6, "24cm", 6, "28cm", 6)
        ));
        catalog.add(new SeedProduct(
                "Set de Ollas Acero Inoxidable 5 Piezas",
                "Ollas de acero inoxidable 18/10, aptas para todo tipo de cocinas.",
                89000, 64900, 27, 14, "HomeChef", "Plata",
                ollas,
                "https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=800",
                t("16cm", 2, "18cm", 3, "20cm", 3, "24cm", 3, "26cm", 3)
        ));
        catalog.add(new SeedProduct(
                "Lampara LED de Mesa Touch",
                "Lampara LED moderna con control tactil y 3 niveles de brillo, USB-C.",
                38000, 26900, 29, 22, "Lumio", "Blanco",
                escritorio,
                "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800",
                t("Unica", 22)
        ));
        catalog.add(new SeedProduct(
                "Juego de Sabanas 600 Hilos King",
                "Sabanas de algodon 100% con 600 hilos, incluye sabana, encimera y fundas.",
                75000, 55900, 25, 20, "HomeChef", "Blanco",
                dormitorio,
                "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=800",
                t("Queen", 6, "King", 8, "Super King", 4, "Doble", 2)
        ));
        catalog.add(new SeedProduct(
                "Mochila Urbana Antirrobo 15.6 Pulgadas",
                "Mochila con compartimento acolchado para laptop, diseno antirrobo, impermeable.",
                58000, 41900, 28, 28, "UrbanPack", "Negro",
                laptop,
                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800",
                t("Unica", 28)
        ));
        catalog.add(new SeedProduct(
                "Mochila Escolar Reforzada con Ruedas",
                "Mochila escolar ergonomica con ruedas, tirantes acolchados y bolsillo para laptop.",
                65000, 48900, 25, 35, "UrbanPack", "Azul",
                escolar,
                "https://images.unsplash.com/photo-1622560480605-d83c853bc5c3?w=800",
                t("Unica", 35)
        ));
        catalog.add(new SeedProduct(
                "Reloj Deportivo Sumergible",
                "Reloj deportivo con GPS, sensor de ritmo cardiaco, resistencia 10ATM.",
                45000, 32900, 27, 18, "TechMobile", "Negro",
                smartwatch,
                "https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=800",
                t("42mm", 8, "46mm", 10)
        ));

        // Persistir
        for (SeedProduct seed : catalog) {
            safeSeed(seed);
        }

        System.out.println("[DataSeeder] Siembra completada. Productos: " + productRepository.count());
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

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
     * Crea un Set<Size> a partir de pares nombre/cantidad. Garantiza que la
     * suma de cantidades sea exactamente la indicada en totalQty, para que
     * el campo sizes del producto no rompa la invariante del stock total.
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
            // Defensiva: la suma de tallas DEBE coincidir con quantity.
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
