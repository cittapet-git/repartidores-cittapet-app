package com.citta.driver.data.api

/**
 * Test stub for [CittaApi]: every endpoint throws until a subclass overrides the ones a
 * given test needs. Keeps individual test fakes small.
 */
open class NotImplementedApi : CittaApi {
    private fun nope(name: String): Nothing = throw NotImplementedError("CittaApi.$name not stubbed for this test")

    override suspend fun login(request: LoginRequest): LoginResponse = nope("login")
    override suspend fun logout(): LogoutResponse = nope("logout")
    override suspend fun registerFcmToken(request: FcmTokenRequest): FcmTokenResponse = nope("registerFcmToken")
    override suspend fun me(): UserDto = nope("me")
    override suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse = nope("changePassword")
    override suspend fun getAlignedDriverStatus(): ApiResponse<DriverStatusData> = nope("getAlignedDriverStatus")
    override suspend fun updateAvailability(request: AvailabilityRequest): ApiResponse<Any> = nope("updateAvailability")
    override suspend fun recordLocation(request: LocationRequest): ApiResponse<TrackingSnapshotDto> = nope("recordLocation")
    override suspend fun getActiveOrders(): ApiResponse<List<PedidoDto>> = nope("getActiveOrders")
    override suspend fun getRecentOrders(): ApiResponse<List<PedidoDto>> = nope("getRecentOrders")
    override suspend fun getMetrics(monthOffset: Int): ApiResponse<MetricsDto> = nope("getMetrics")
    override suspend fun getHistorial(year: Int, month: Int): ApiResponse<List<PedidoDto>> = nope("getHistorial")
    override suspend fun searchHistorial(query: String): ApiResponse<List<PedidoDto>> = nope("searchHistorial")
    override suspend fun iniciarViaje(orderId: Int): ApiResponse<PedidoDto> = nope("iniciarViaje")
    override suspend fun marcarEntregado(orderId: Int): ApiResponse<PedidoDto> = nope("marcarEntregado")
    override suspend fun reportIncident(orderId: Int, request: IncidentRequest): ApiResponse<IncidentDto> = nope("reportIncident")
    override suspend fun getPedidoDetail(orderId: Int): PedidoDetailResponse = nope("getPedidoDetail")
    override suspend fun getPedidoEvents(orderId: Int): ApiResponse<List<OrderEventDto>> = nope("getPedidoEvents")
    override suspend fun resolveGeo(url: String): ApiResponse<GeoPointDto> = nope("resolveGeo")
    override suspend fun getTrackingSnapshot(orderId: Int): ApiResponse<TrackingSnapshotDto> = nope("getTrackingSnapshot")
}
