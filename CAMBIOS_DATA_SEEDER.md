# Solución de persistencia del catálogo (DataSeeder)

## Resumen ejecutivo

| | |
|---|---|
| **Síntoma** | Tras cada redeploy/cold-start en Render, los productos vuelven a su estado roto (10 productos con tallas vacías, sin tallas reales). El carrito se rompe porque `ProductDetail` filtra tallas con `quantity > 0` y no encuentra ninguna. |
| **Causa raíz** | El backend usa **H2 in-memory** (`spring.datasource.url=jdbc:h2:mem:shopwavefusion`) en el perfil `prod`. Render, en su plan gratuito, hace *spin-down* del servicio tras 15 min de inactividad; cada vez que recibe una nueva petición, **la JVM se reinicia y la base de datos se vacía**. |
| **Síntoma secundario** | El endpoint `/admin/products/creates` (que en frontend llama `AdminProductService.createMultiple`) **requiere Basic Auth** y no JWT. Por eso el frontend nunca podría re-sembrar el catálogo aunque quisiéramos. |
| **Solución elegida** | `CommandLineRunner` de Spring Boot que siembra el catálogo completo de **30 productos con tallas reales y stock correlacionado** cada vez que arranca la JVM, mientras la BD esté vacía. |
| **Resultado** | El catálogo está disponible en los primeros milisegundos de cualquier arranque del backend, sin intervención manual, después de cualquier spin-down/redeploy. |

## Por qué esta y no otras opciones

| Alternativa | Por qué se descartó |
|---|---|
| Migrar a PostgreSQL en Render | Solución correcta a largo plazo, pero requiere: cambiar driver (`mysql-connector-java` → `postgresql`), reconfigurar `application-prod.properties`, levantar servicio de Postgres, migrar datos. Cambia el contrato de despliegue, no es trivial de revertir. |
| Migrar a Supabase/Neon (DBaaS externo) | Igual que arriba + depende de un servicio externo que también puede dormirse en su plan gratuito. |
| Persistent Disk de Render | Solo disponible en planes pagos. |
| Seed desde frontend (botón "Re-sembrar catálogo") | **No funciona**: el endpoint `/admin/products/creates` exige Basic Auth, no JWT. El admin no puede disparar el seed. Además requiere acción manual. |
| data.sql / import.sql de Spring | Funciona, pero menos flexible que un `CommandLineRunner` Java: más difícil de versionar lógica condicional, debugging limitado, dependencias circulares. |

`CommandLineRunner` es el mecanismo nativo de Spring Boot para ejecutar lógica al arranque, con pleno acceso a repositorios, transacciones y logging. Es la opción más profesional y la que menos cambia la superficie existente.

## Qué se modificó

**Un solo archivo:** `backend/src/main/java/com/shopwavefusion/config/DataSeeder.java`

Se reescribió por completo el cuerpo del método `run(...)` del `DataSeeder` existente. La firma, los imports de Spring, la anotación `@Component @Order(2)`, el patrón `if (count > 0) return;` y los helpers `ensureCategory` / `safeSeed` se **preservaron**; se reemplazó el catálogo de 10 productos por uno de 30 con tallas reales.

### Cambios puntuales

1. **Catálogo ampliado de 10 → 30 productos** distribuidos en 6 categorías top-level (Ropa, Deportes, Calzado, Electrónica, Hogar, Accesorios) y 3 niveles de profundidad.
2. **Cada producto declara tallas** mediante un helper `t("S", 10, "M", 15, ...)` que construye un `Set<Size>` con nombre y cantidad. Las tallas pueden ser String (`S`, `M`, `XL`) o Integer-like String (`"28"`, `"38"`, `"128GB"`, `"41mm"`) según el modelo.
3. **Invariante stock = Σ(tallas) garantizada.** El método `safeSeed` recalcula la suma de las cantidades por talla y, si no coincide con el campo `quantity`, ajusta `quantity` y loguea un warning. Esto replica la validación que el frontend hace en `ProductForm.tsx` y evita que el catálogo sembrado rompa el carrito.
4. **Jerarquía de categorías completa** que soporta los 30 productos. El método `ensureCategory` ya es idempotente (busca por nombre antes de crear).
5. **`AdminInitializer` no se tocó**: el admin seed es independiente y sigue corriendo (`admin@example.com / admin`).

### Invariante crítica

```java
int sizesTotal = seed.sizes.stream().mapToInt(Size::getQuantity).sum();
if (sizesTotal != seed.quantity) {
    System.err.println("[DataSeeder] Mismatch stock/tallas en '"
            + seed.title + "': stock=" + seed.quantity + " sizes=" + sizesTotal);
    seed.quantity = sizesTotal;   // autocorrección
}
```

Si por error de mantenimiento alguien declara tallas que no suman el stock total, el seeder lo detecta y se autocorrige en lugar de sembrar datos inconsistentes.

## Cómo funciona end-to-end

1. **Render** recibe una petición → despierta la JVM.
2. Spring Boot arranca, carga el perfil `prod` (H2 in-memory, `jdbc:h2:mem:shopwavefusion;DB_CLOSE_DELAY=-1;MODE=MySQL`).
3. JPA crea las tablas vacías gracias a `spring.jpa.hibernate.ddl-auto=update`.
4. `AdminInitializer` se ejecuta (`@Order` por defecto, antes de los demás) y crea el usuario admin si no existe.
5. **`DataSeeder` se ejecuta** (`@Order(2)`), detecta que la tabla de productos está vacía, y siembra las categorías + 30 productos en una sola transacción.
6. La primera petición entrante ya encuentra los 30 productos con tallas correctas y stock correlacionado.

## Catálogo sembrado (resumen)

| Categoría | Productos |
|---|---|
| Ropa (Camisetas, Polos, Camisas, Pantalones, Vestidos, Blazers, Chaquetas, Accesorios) | 11 |
| Calzado (Zapatillas, Botines, Zapatos, Sandalias) | 6 |
| Deportes (Running H/M, Yoga M) | 3 |
| Electrónica (Auriculares, Smartphones, Tablets, Smart TV, Smartwatch) | 6 |
| Hogar (Sartenes, Ollas, Lámpara, Sábanas) | 4 |
| Total | **30** |

**Stock total agregado: ~680 unidades** distribuidas en tallas reales (S/M/L/XL para ropa, 28-44 para calzado, 128GB/256GB para smartphones, etc.).

## Cómo desplegar / probar

### Opción A: Compilar y desplegar manualmente

```bash
cd C:\Proyectos\shopwave-entorno\backend
# Compilar (requiere Java 17 instalado y JAVA_HOME apuntando a él)
mvn clean package -DskipTests
# El JAR queda en target/shopwavefusionbackend-0.0.1-SNAPSHOT.jar
```

Luego en Render, en el servicio backend:
- Conectar el repo o subir el JAR
- Confirmar variable de entorno `SPRING_PROFILES_ACTIVE=prod`
- Redeploy

### Opción B: Si Render está conectado a GitHub (más probable)

1. Commit y push del cambio a `DataSeeder.java` en la rama que Render tenga configurada (típicamente `master` o `main`).
2. Render detectará el push y redesplegará automáticamente.
3. Verificar en los logs de Render que aparecen las líneas:
   ```
   [DataSeeder] Iniciando siembra de catalogo (30 productos)...
   [DataSeeder] Producto creado: Camiseta Esencial Algodón (stock=45, tallas=4)
   [DataSeeder] Producto creado: Polo Pique Premium Hombre (stock=30, tallas=4)
   ...
   [DataSeeder] Siembra completada. Productos: 30
   ```

### Verificación en el frontend

Una vez desplegado, refrescar `https://shopwavefront.netlify.app/products`:
- Deben aparecer los 30 productos en el catálogo.
- Al entrar al detalle de cualquier producto, **deben verse las tallas** (no debe estar vacío).
- "Agregar al carrito" debe funcionar después de loguearse.

Para forzar el ciclo completo (probar el caso de cold-start) desde local:
```bash
curl -X POST "https://shopwave-backend-ky66.onrender.com/logout"  # forzar el cierre
sleep 60  # esperar a que Render duerma el servicio
curl "https://shopwave-backend-ky66.onrender.com/products"  # lo reactiva; debería devolver 30 productos con tallas
```

## Garantías

- **Cero intervención manual** después del deploy: el siguiente spin-down se recupera solo.
- **Idempotencia**: si el admin agregó productos custom y la JVM sigue viva, el seeder no se vuelve a ejecutar (`if (count > 0) return;`). Solo se ejecuta en arranques frescos (H2 vacía).
- **Sin pisar datos del admin en caliente**: misma garantía que el seeder anterior.
- **Sin cambios en la BD ni en el esquema**: la estructura JPA es la misma; solo cambia el contenido sembrado.
- **Sin nuevos endpoints ni cambios en controllers**: no hay regresiones para el frontend.

## Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| El seeder se ejecuta pero el `AdminInitializer` falla → BD inconsistente | Ambos corren en orden (`AdminInitializer` sin `@Order` corre primero, `DataSeeder` con `@Order(2)` después). Cada uno tiene `try/catch` por producto/categoría. |
| El admin crea productos custom → siguiente cold-start los pierde | Es la naturaleza de H2 in-memory. Esta solución no lo cambia. Para resolverlo de verdad, migrar a Postgres (ver tabla de alternativas). |
| Una imagen externa (Unsplash) cambia de URL | Las URLs son permanentes (`?w=800`). Si se rompen, reemplazarlas en el array `catalog` y redeploy. |
| El admin quiere re-sembrar el catálogo en caliente | Botón "Eliminar todos + reiniciar backend" sigue siendo la única vía. No se agregó botón de re-seed desde el frontend porque el endpoint exige Basic Auth. |

## Próximo paso sugerido (fuera de alcance de este PR)

Para resolver el problema de raíz (pérdida de datos en general, no solo del catálogo), migrar de H2 in-memory a **PostgreSQL** (Render ofrece un free tier de 90 días, o Supabase/Neon como alternativa persistente). Esto requerirá:
- Agregar `org.postgresql:postgresql` a `pom.xml`
- Cambiar `spring.datasource.url` a `jdbc:postgresql://...`
- Quitar `MODE=MySQL` (H2-specific)
- Eliminar el `DataSeeder` (los datos persistirían)

Pero para esta entrega, el `DataSeeder` resuelve el 100% del síntoma reportado (catálogo roto tras spin-down) sin tocar infraestructura.
