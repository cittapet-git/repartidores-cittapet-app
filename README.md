# App Android para Repartidores (Citta Tracking)

Cliente Android del sistema de tracking para repartidores de CittaPet.

## Stack

- Kotlin + Jetpack Compose
- Hilt (DI)
- Retrofit + OkHttp (HTTP)
- Room (historial local de notificaciones)
- Fused Location Provider (GPS real)
- Firebase (FCM + Crashlytics)
- WorkManager (replay offline outbox)

## Estructura

```
app/src/main/java/com/citta/driver/
  data/          — adaptadores (API, repositorios, mappers)
  domain/        — puertos y modelos de dominio
  ui/            — pantallas Compose y ViewModels
  service/       — TrackingService, FCM, BootReceiver, workers
  di/            — grafo Hilt
  permissions/   — flujo de permisos de ubicación
```

## Configuración de secretos

### Firebase (google-services.json)

Este archivo **NO** está versionado. Para obtenerlo:

1. Ir a [Firebase Console](https://console.firebase.google.com/)
2. Proyecto: `cittapet-tracking`
3. Configuración del proyecto → Apps Android → `com.citta.driver`
4. Descargar `google-services.json`
5. Colocar en: `app/google-services.json`

### Maps API Key

Crear `local.properties` en la raíz del proyecto:

```properties
MAPS_API_KEY=YOUR_KEY_HERE
```

O usar `-PMAPS_API_KEY=YOUR_KEY` al compilar.

## Build

### Debug (desarrollo)

```bash
./gradlew assembleDebug
```

Por defecto apunta a `http://192.168.2.15:8080/`. Override:

```bash
./gradlew assembleDebug -PAPI_BASE_URL=http://tu-host:puerto/
```

### Release (producción)

```bash
./gradlew assembleRelease -PAPI_BASE_URL=https://repartidores.cittapet.app/
```

## Tests

```bash
./gradlew testDebugUnitTest
```

51 archivos de test, 242 anotaciones `@Test`. Cubren API/DTO, auth, mappers, outbox, sesión, tracking, permisos, dominio, navegación y ViewModels.

## Contrato API

La app consume el backend en `repartidores-cittapet` (repo separado).
Rutas principales en `app/src/main/java/com/citta/driver/data/api/CittaApi.kt`.

## Dominio productivo

- API backend: `https://repartidores.cittapet.app/`
- Web dashboard: `https://repartidores.cittapet.app/` (same-origin)
