# ShopWave backend

Backend REST de la versión rework de ShopWave. La aplicación expone un contrato versionado bajo `/api/v1`, persiste en MySQL mediante Flyway y utiliza JWT Bearer entre clientes autorizados y la API. El frontend oficial usa un BFF de Next.js, por lo que el JWT no se entrega a JavaScript del navegador.

## Stack

- Java 17 y Spring Boot 3.1.2
- Spring Web, Validation, Security y Data JPA
- MySQL 8 y Flyway
- Maven Wrapper
- JUnit 5, MockMvc y H2 aislado para la suite local

## Ejecución local

1. Copia `.env.example` a un entorno local seguro y sustituye sus placeholders sin subir el archivo real.
2. Crea una base MySQL nueva llamada `shopwave_rework`.
3. Arranca desde `backend`:

```powershell
$env:DB_HOST='localhost'
$env:DB_PORT='3306'
$env:DB_NAME='shopwave_rework'
$env:DB_USER='shopwave'
$env:DB_PASSWORD='valor-local'
$env:JWT_SECRET='genera-un-secreto-local-de-32-bytes-o-mas'
$env:APP_ORIGIN='http://localhost:3000'
./mvnw.cmd spring-boot:run
```

La configuración no crea ni modifica una base legacy: `ddl-auto=validate` y Flyway aplican sólo `db/migration` sobre la base configurada explícitamente. Para datos ficticios se puede habilitar `SHOPWAVE_SEED_ENABLED=true`; el admin demo requiere además una contraseña introducida en el entorno local.

## Contrato

La especificación normativa está en [`backend/openapi/shopwave-v1.yaml`](backend/openapi/shopwave-v1.yaml). Swagger se sirve en `/swagger-ui/index.html` cuando la aplicación está levantada y los endpoints funcionales son los de `/api/v1`.

Los pagos son `MOCK/SIMULATED`: no existe un flujo de tarjeta, PAN, CVV ni proveedor de cobro.

## Verificación

```powershell
./mvnw.cmd test
./mvnw.cmd verify -Pintegration
```

La suite local usa una base H2 efímera para feedback rápido. La equivalencia completa de MySQL/Testcontainers requiere Docker y debe ejecutarse en un entorno aislado; no se debe conectar a una base real o legacy.

## Docker

`docker-compose.yml` está preparado para una base `shopwave_rework` nueva y exige variables de entorno. No ejecutes `down -v` sobre volúmenes que contengan datos que quieras conservar.

## Estado de demo

No se declara una demo publicada ni credenciales universales. Las capturas, resultados de Lighthouse y pruebas full-stack deben generarse en un entorno local o sandbox y documentarse sin publicar secretos ni datos personales.
