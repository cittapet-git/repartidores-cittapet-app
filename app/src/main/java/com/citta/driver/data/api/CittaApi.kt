package com.citta.driver.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class LoginRequest(val email: String, val password: String)

/** Flat login payload (NOT wrapped in `data`): `{ token, expires_at, user, identity_source }`. */
data class LoginResponse(
    val token: String,
    val expires_at: Long,
    val user: UserDto,
    val identity_source: String? = null,
)
data class LogoutResponse(val ok: Boolean)

/** Body for `POST /api/v1/auth/change-password` (snake_case keys, flat — not wrapped in `data`). */
data class ChangePasswordRequest(val current_password: String, val new_password: String)

/** `POST /api/v1/auth/change-password` returns a flat `{ "ok": true }`. */
data class ChangePasswordResponse(val ok: Boolean = true)

/**
 * Backend user shape shared by `login.user` and `/auth/me`. Both `role` and `rol` carry the
 * same value; the app prefers `rol`. `email` and `tipo` may be null.
 */
data class UserDto(
    val id: Int,
    val name: String,
    val email: String? = null,
    val role: String? = null,
    val rol: String? = null,
    val tipo: String? = null,
    val foto_url: String? = null,
    val must_change_password: Boolean = false,
    val creado_en: String? = null,
    val total_pedidos_despachados: Int? = null,
)
data class AvailabilityRequest(val availability_state: String)

/** Aligned `GET /api/v1/driver/status` payload (wrapped in `data`). `shift_state` is the tracking source of truth. */
data class DriverStatusData(
    val shift_state: String,
    val active_order_ids: List<Int> = emptyList(),
)

data class FcmTokenRequest(
    val token: String,
    val platform: String = "android"
)

/** `POST /api/v1/auth/fcm-token` returns a flat `{ "ok": true }` (not wrapped in `data`). */
data class FcmTokenResponse(val ok: Boolean = true)

data class LocationRequest(
    val order_id: Int? = null,
    val latitude: Double,
    val longitude: Double,
    val captured_at: String
)

data class PositionDto(
    val latitude: Double,
    val longitude: Double,
    val captured_at: String
)

/**
 * Slimmed to only what the app consumes from `POST /api/v1/driver/location`: the trip status
 * for the tracking line and the echoed driver location. The backend still returns `order`,
 * `latest_position`, `history`, and `active_order_ids`, which Gson simply ignores here.
 */
data class TrackingSnapshotDto(
    val trip_status: String,
    val driver_location: PositionDto? = null
)

data class ApiResponse<T>(val data: T)

/** `GET /api/v1/driver/metricas` — driver trip metrics for the "Centro de Métricas" screen. */
data class MetricsDto(
    val entregados_hoy: Int = 0,
    val duracion_promedio_min: Int = 0,
    val ganancias_semana: Double = 0.0,
    /** 7 delivered counts, Monday..Sunday, current ISO week. */
    val serie_semanal: List<Int> = emptyList(),
    /** 5 delivered counts, one per week-bucket of the selected month. */
    val serie_mensual: List<Int> = emptyList(),
)

// ─── Aligned active-order shapes (Slice 2) ───────────────────────────────────
// Source of truth: repartidores-cittapet OrderMapper::map + IniciarViajeUseCase +
// MarcarEntregadoUseCase + ReportarIncidenteUseCase.

/** Backend `OrderShape` from `OrderMapper::map`, wrapped in `data` by every driver order endpoint. */
data class PedidoDto(
    val id: Int,
    val source_ref: String? = null,
    /** WooCommerce order id when the order came from WooCommerce; null for manual orders. */
    val wc_order_id: Int? = null,
    val source_type: String? = null,
    val tipo_pedido: String? = null,
    val item_count: Int? = null,
    val package_weight_kg: Double? = null,
    val maps_link: String? = null,
    val customer_name: String? = null,
    val customer_phone: String? = null,
    val delivery_address: String? = null,
    val delivery_notes: String? = null,
    val total_amount: Double = 0.0,
    val pago_doble: Int = 0,
    val requiere_hoja_firmada: Int = 0,
    /**
     * Raw backend metadata blob; left opaque on purpose. The backend `OrderMapper::map` emits
     * this as a decoded JSON value (object/array/null), NOT a string, so it must be typed as a
     * generic [com.google.gson.JsonElement] — a `String?` here throws `JsonSyntaxException`
     * ("Expected a string but was BEGIN_OBJECT") for any order that carries metadata.
     */
    val metadata: com.google.gson.JsonElement? = null,
    val status: String? = null,
    val assigned_driver_user_id: Int? = null,
    val assigned_driver_user_ids: List<Int> = emptyList(),
    val created_at: String? = null,
    val updated_at: String? = null,
)

/** Request body for `POST /api/v1/pedidos/{id}/incidentes`. `tipo` required; the rest optional. */
data class IncidentRequest(
    val tipo: String,
    val descripcion: String? = null,
    val metadata: Map<String, Any>? = null,
)

/** Lean projection of the created incident (`{ "data": <incident> }`). */
data class IncidentDto(
    val id: Int? = null,
    val tipo: String? = null,
    val descripcion: String? = null,
)

/** Order event/history row (`GET /api/v1/pedidos/{id}/eventos`). Shape kept opaque until consumed. */
data class OrderEventDto(
    val id: Int? = null,
    val tipo: String? = null,
    val descripcion: String? = null,
    val created_at: String? = null,
)

/** `GET /api/v1/pedidos/{id}` returns the order plus an `events` array alongside `data`. */
data class PedidoDetailResponse(
    val data: PedidoDto,
    val events: List<OrderEventDto> = emptyList(),
)

interface CittaApi {
    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/api/v1/auth/logout")
    suspend fun logout(): LogoutResponse

    @POST("/api/v1/auth/fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): FcmTokenResponse

    @GET("/api/v1/auth/me")
    suspend fun me(): UserDto

    @POST("/api/v1/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ChangePasswordResponse

    /** Aligned status read used by [com.citta.driver.data.driver.DefaultDriverRepository]. */
    @GET("/api/v1/driver/status")
    suspend fun getAlignedDriverStatus(): ApiResponse<DriverStatusData>

    @PATCH("/api/v1/driver/availability")
    suspend fun updateAvailability(@Body request: AvailabilityRequest): ApiResponse<Any>

    @POST("/api/v1/driver/location")
    suspend fun recordLocation(@Body request: LocationRequest): ApiResponse<TrackingSnapshotDto>

    // ─── Aligned active-order endpoints (Slice 2) ───────────────────────────────
    @GET("/api/v1/driver/pedidos-activos")
    suspend fun getActiveOrders(): ApiResponse<List<PedidoDto>>

    @GET("/api/v1/driver/pedidos-recientes")
    suspend fun getRecentOrders(): ApiResponse<List<PedidoDto>>

    @GET("/api/v1/driver/metricas")
    suspend fun getMetrics(@Query("monthOffset") monthOffset: Int): ApiResponse<MetricsDto>

    /** `GET /api/v1/driver/historial` — one calendar month of delivered trips, newest first. */
    @GET("/api/v1/driver/historial")
    suspend fun getHistorial(
        @Query("year") year: Int,
        @Query("month") month: Int,
    ): ApiResponse<List<PedidoDto>>

    /** `GET /api/v1/driver/historial/buscar` — up to 5 delivered trips (last ~20 months) matching [query]. */
    @GET("/api/v1/driver/historial/buscar")
    suspend fun searchHistorial(@Query("q") query: String): ApiResponse<List<PedidoDto>>

    @POST("/api/v1/pedidos/{id}/iniciar-viaje")
    suspend fun iniciarViaje(@Path("id") orderId: Int): ApiResponse<PedidoDto>

    @POST("/api/v1/pedidos/{id}/marcar-entregado")
    suspend fun marcarEntregado(@Path("id") orderId: Int): ApiResponse<PedidoDto>

    @POST("/api/v1/pedidos/{id}/incidentes")
    suspend fun reportIncident(@Path("id") orderId: Int, @Body request: IncidentRequest): ApiResponse<IncidentDto>

    @GET("/api/v1/pedidos/{id}")
    suspend fun getPedidoDetail(@Path("id") orderId: Int): PedidoDetailResponse

    @GET("/api/v1/pedidos/{id}/eventos")
    suspend fun getPedidoEvents(@Path("id") orderId: Int): ApiResponse<List<OrderEventDto>>

    @GET("/api/v1/orders/{id}/tracking")
    suspend fun getTrackingSnapshot(@Path("id") orderId: Int): ApiResponse<TrackingSnapshotDto>
}
