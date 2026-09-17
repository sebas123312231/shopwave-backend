# ShopWave API v1

Implementación Spring Boot del rework de ShopWave. El código activo vive bajo `com.shopwavefusion.rework`; el contrato REST está en [`openapi/shopwave-v1.yaml`](openapi/shopwave-v1.yaml).

## Comandos

```powershell
./mvnw.cmd test
./mvnw.cmd verify -Pintegration
```

`application-test.properties` usa H2 efímero y un secreto exclusivo de tests. No representa paridad de producción con MySQL. Para una verificación de persistencia real se necesita un MySQL de test aislado o Docker; nunca se usan datos del dueño del proyecto.

## Variables mínimas

La aplicación requiere `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` y `APP_ORIGIN` cuando corre con los perfiles `local`, `demo` o `prod`. `JWT_SECRET` debe tener al menos 32 bytes. Los seeders y el admin demo están desactivados por defecto.

## API y seguridad

- `POST /api/v1/auth/register`, `POST /api/v1/auth/login` y `POST /api/v1/auth/logout`.
- Catálogo público paginado en `products`, `categories` y `products/facets`.
- Perfil, direcciones, carrito y órdenes requieren Bearer válido.
- Admin requiere `ROLE_ADMIN` y validación de versión para cambios.
- CORS acepta únicamente los orígenes configurados.
- Las respuestas de error son `application/problem+json`.
- Checkout calcula totales en servidor, usa locks, snapshots e `Idempotency-Key`.
- El pago es explícitamente `MOCK/SIMULATED`; nunca se reciben datos de tarjeta.

## Datos demo

Activa `SHOPWAVE_SEED_ENABLED=true` sólo contra una base demo nueva. Para crear un admin demo se requieren `SHOPWAVE_DEMO_ADMIN_ENABLED=true` y `SHOPWAVE_DEMO_ADMIN_PASSWORD` en el entorno local. No se imprimen ni se documentan credenciales.
