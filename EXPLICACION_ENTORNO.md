# Entorno ShopWave rework

Este directorio contiene el backend Spring Boot en `backend/`, el Dockerfile y una composición local para una base MySQL nueva. No representa ni migra la base de datos legacy.

## Flujo de ejecución

```text
navegador -> Next.js BFF (puerto 3000) -> Spring Boot (puerto 8080) -> MySQL (puerto 3306)
```

El BFF mantiene el JWT en una cookie HttpOnly y reenvía un Bearer al backend. La API también puede probarse directamente con Bearer desde un cliente autorizado. El contrato completo se encuentra en `backend/openapi/shopwave-v1.yaml`.

## Variables

Usa `.env.example` como lista de nombres, pero crea los valores reales sólo en tu entorno local. La aplicación exige credenciales explícitas para perfiles MySQL y un `JWT_SECRET` de al menos 32 bytes. No guardes valores reales en Git, capturas, logs o documentación.

`SHOPWAVE_SEED_ENABLED` y `SHOPWAVE_DEMO_ADMIN_ENABLED` están desactivados por defecto. El seed y el admin demo sólo deben usarse contra una base demo nueva.

## Docker Compose

`docker-compose.yml` arranca MySQL 8 y el backend con `shopwave_rework`, Flyway y `ddl-auto=validate`. El healthcheck espera la disponibilidad de MySQL antes de arrancar Spring. La contraseña y el secreto se interpolan desde el entorno; el compose no contiene credenciales operativas.

```powershell
Copy-Item .env.example .env
# Edita .env sólo localmente
docker compose --env-file .env up --build
```

No ejecutes `docker compose down -v` si el volumen contiene datos que quieras conservar. No se autoriza conectar este compose a una base remota o real.

## Verificación

Desde `backend/`:

```powershell
./mvnw.cmd test
./mvnw.cmd verify -Pintegration
```

La suite actual se ejecuta con H2 efímero para feedback local. La verificación MySQL/Testcontainers requiere Docker disponible y debe documentarse como `NO VERIFICADO` si no se ejecuta en un entorno aislado.
