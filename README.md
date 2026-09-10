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

Requerida para el mapa nativo de la pantalla de detalle de pedido. **Nunca** se versiona.

- Local: agregar a `local.properties` (gitignored) en la raíz del proyecto:

  ```properties
  MAPS_API_KEY=YOUR_KEY_HERE
  ```

  o pasar `-PMAPS_API_KEY=YOUR_KEY` al compilar. `build.gradle.kts` la inyecta en el
  placeholder `com.google.android.geo.API_KEY` del manifest; si falta, el mapa sale en blanco.
- CI: viene del secret de repositorio `MAPS_API_KEY` (ver `.github/workflows/android-ci.yml`).
- La clave viaja dentro del APK, así que su protección real es en Google Cloud Console:
  restricción de aplicación al paquete `com.citta.driver` + SHA-1 (debug y release),
  restricción de API a "Maps SDK for Android", y tope de cuota.

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
