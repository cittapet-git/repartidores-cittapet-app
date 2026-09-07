package com.citta.driver.ui.historial

import com.citta.driver.domain.driver.DriverMetrics
import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.driver.DriverStatus
import com.citta.driver.domain.driver.HistorialTrip
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.IncidentInput
import com.citta.driver.util.MainDispatcherRule
import java.time.YearMonth
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistorialViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun trip(id: Int) = HistorialTrip(
        id = id,
        orderNumber = "55000$id",
        customerName = "Cliente $id",
        deliveryAddress = "Calle $id",
        completedAt = "2026-09-0$id 12:00:00",
    )

    private class FakeDriverRepository : DriverRepository {
        var historialResult: List<HistorialTrip> = emptyList()
        var historialError: Throwable? = null
        var searchResult: List<HistorialTrip> = emptyList()

        var lastHistorialArgs: Pair<Int, Int>? = null
        var historialCallCount = 0
        var lastSearchQuery: String? = null
        var searchCallCount = 0

        override suspend fun getHistorial(year: Int, month: Int): List<HistorialTrip> {
            historialCallCount++
            lastHistorialArgs = year to month
            historialError?.let { throw it }
            return historialResult
        }

        override suspend fun searchHistorial(query: String): List<HistorialTrip> {
            searchCallCount++
            lastSearchQuery = query
            return searchResult
        }

        override suspend fun getStatus(): DriverStatus = DriverStatus(ShiftState.OFF_SHIFT, emptyList())
        override suspend fun setShift(state: ShiftState): DriverStatus = getStatus()
        override suspend fun getActiveOrders(): List<ActiveOrder> = emptyList()
        override suspend fun getRecentOrders(): List<ActiveOrder> = emptyList()
        override suspend fun getMetrics(monthOffset: Int): DriverMetrics =
            DriverMetrics(0, 0, 0.0, emptyList(), emptyList())
        override suspend fun startTrip(orderId: Int): ActiveOrder = error("unused")
        override suspend fun markDelivered(orderId: Int): ActiveOrder = error("unused")
        override suspend fun reportIncident(orderId: Int, input: IncidentInput) = Unit
        override suspend fun getOrderDetail(orderId: Int): ActiveOrder = error("unused")
    }

    private fun viewModel(repo: FakeDriverRepository) = HistorialViewModel(repo)

    @Test
    fun `loads the current month on init`() = runTest {
        val repo = FakeDriverRepository().apply { historialResult = listOf(trip(1), trip(2)) }

        val vm = viewModel(repo)
        advanceUntilIdle()

        val now = YearMonth.now()
        assertEquals(now, vm.selectedMonth)
        assertEquals(now.year to now.monthValue, repo.lastHistorialArgs)
        assertEquals(2, vm.trips.size)
        assertFalse(vm.isLoading)
        assertNull(vm.loadError)
    }

    @Test
    fun `a load failure clears the list and surfaces an error`() = runTest {
        val repo = FakeDriverRepository().apply { historialError = RuntimeException("boom") }

        val vm = viewModel(repo)
        advanceUntilIdle()

        assertTrue(vm.trips.isEmpty())
        assertFalse(vm.isLoading)
        assertTrue(vm.loadError != null)
    }

    @Test
    fun `selectMonth reloads for the chosen month and closes the picker`() = runTest {
        val repo = FakeDriverRepository()
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.openMonthPicker()
        vm.selectMonth(2026, 3)
        advanceUntilIdle()

        assertEquals(YearMonth.of(2026, 3), vm.selectedMonth)
        assertEquals(2026 to 3, repo.lastHistorialArgs)
        assertTrue(vm.monthTitle.startsWith("Marzo 2026"))
        assertFalse(vm.monthPickerVisible)
    }

    @Test
    fun `selectMonth clamps a month older than the allowed window`() = runTest {
        val repo = FakeDriverRepository()
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.selectMonth(1990, 1)
        advanceUntilIdle()

        assertEquals(vm.earliestMonth, vm.selectedMonth)
    }

    @Test
    fun `entering search blanks the results and lifts the field`() = runTest {
        val repo = FakeDriverRepository()
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.enterSearch()

        assertTrue(vm.isSearching)
        assertEquals("", vm.searchQuery)
        assertTrue(vm.searchResults.isEmpty())
    }

    @Test
    fun `a query hits the search endpoint after debounce and is capped at five results`() = runTest {
        val repo = FakeDriverRepository().apply { searchResult = (1..8).map { trip(it) } }
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.enterSearch()

        vm.onSearchQueryChange("carlos")
        advanceUntilIdle()

        assertEquals("carlos", repo.lastSearchQuery)
        assertEquals(5, vm.searchResults.size)
        assertFalse(vm.isSearchLoading)
    }

    @Test
    fun `a blank query clears results without calling the endpoint`() = runTest {
        val repo = FakeDriverRepository().apply { searchResult = listOf(trip(1)) }
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.enterSearch()
        vm.onSearchQueryChange("carlos")
        advanceUntilIdle()
        val callsAfterFirstQuery = repo.searchCallCount

        vm.onSearchQueryChange("   ")
        advanceUntilIdle()

        assertEquals(callsAfterFirstQuery, repo.searchCallCount)
        assertTrue(vm.searchResults.isEmpty())
    }

    @Test
    fun `exiting search resets its state`() = runTest {
        val repo = FakeDriverRepository().apply { searchResult = listOf(trip(1)) }
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.enterSearch()
        vm.onSearchQueryChange("carlos")
        advanceUntilIdle()

        vm.exitSearch()

        assertFalse(vm.isSearching)
        assertEquals("", vm.searchQuery)
        assertTrue(vm.searchResults.isEmpty())
    }
}
