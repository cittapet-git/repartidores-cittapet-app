package com.citta.driver.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.citta.driver.domain.observability.AppStatus
import com.citta.driver.ui.ajustes.AjustesScreen
import com.citta.driver.ui.components.CittaBottomNav
import com.citta.driver.ui.historial.HistorialScreen
import com.citta.driver.ui.home.HomeScreen
import com.citta.driver.ui.metrics.MetricsScreen
import com.citta.driver.ui.notificaciones.NotificacionesScreen
import com.citta.driver.ui.orderdetail.OrderDetailScreen
import com.citta.driver.ui.status.AppStatusScreen

/**
 * Every driver-app route. `home` and `detalle/{orderId}` are implemented; `historial`,
 * `ajustes` and `notificaciones` have real screens; the rest render [PlaceholderScreen].
 */
object DriverRoute {
    const val HOME = "home"
    const val METRICAS = "metricas"
    const val HISTORIAL = "historial"
    const val AJUSTES = "ajustes"
    const val NOTIFICACIONES = "notificaciones"
    const val DETALLE_PATTERN = "detalle/{orderId}"
    const val DETALLE_ARG = "orderId"

    const val STATUS_PATTERN = "status/{statusName}"
    const val STATUS_ARG = "statusName"

    /** Concrete detail route for one order, e.g. `detalle(42) == "detalle/42"`. */
    fun detalle(orderId: Int): String = "detalle/$orderId"

    /** Concrete full-screen status route for a blocking [AppStatus], e.g. `status/SessionExpired`. */
    fun status(status: AppStatus): String = "status/${status.name}"

    /** Human title for a non-home route (path prefix, args ignored). */
    fun titleFor(route: String?): String = when (route?.substringBefore("/")) {
        HISTORIAL -> "Historial"
        AJUSTES -> "Ajustes"
        NOTIFICACIONES -> "Notificaciones"
        "detalle" -> "Detalle del pedido"
        else -> "En construcción"
    }
}

/** Left-to-right order of the bottom-nav tabs; drives the directional slide between them. */
private val TAB_ORDER = listOf(DriverRoute.HOME, DriverRoute.METRICAS, DriverRoute.AJUSTES)

private const val NAV_ANIM_MS = 280

private fun tabIndexOf(route: String?): Int = TAB_ORDER.indexOf(route?.substringBefore("/"))

/**
 * Horizontal slide when moving between two bottom-nav tabs, in the direction of
 * their order in [TAB_ORDER] (later tab → enters from the right). Returns `null`
 * for any navigation that is not tab-to-tab, so the caller falls back to a fade.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabSlideEnter(): EnterTransition? {
    val from = tabIndexOf(initialState.destination.route)
    val to = tabIndexOf(targetState.destination.route)
    if (from < 0 || to < 0 || from == to) return null
    val goingRight = to > from
    return slideInHorizontally(tween(NAV_ANIM_MS)) { width -> if (goingRight) width else -width }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabSlideExit(): ExitTransition? {
    val from = tabIndexOf(initialState.destination.route)
    val to = tabIndexOf(targetState.destination.route)
    if (from < 0 || to < 0 || from == to) return null
    val goingRight = to > from
    return slideOutHorizontally(tween(NAV_ANIM_MS)) { width -> if (goingRight) -width else width }
}

@Composable
fun DriverNavHost(
    onLogout: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    // Shared bottom-nav tab selector: keeps the back stack shallow ([HOME] or [HOME, tab]).
    val selectTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            launchSingleTop = true
            popUpTo(DriverRoute.HOME)
        }
    }

    val currentRoute by navController.currentBackStackEntryAsState()
    val currentTab = currentRoute?.destination?.route
    val showNavBar = currentTab in TAB_ORDER

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = DriverRoute.HOME,
            enterTransition = { tabSlideEnter() ?: fadeIn(tween(NAV_ANIM_MS)) },
            exitTransition = { tabSlideExit() ?: fadeOut(tween(NAV_ANIM_MS)) },
            popEnterTransition = { tabSlideEnter() ?: fadeIn(tween(NAV_ANIM_MS)) },
            popExitTransition = { tabSlideExit() ?: fadeOut(tween(NAV_ANIM_MS)) },
        ) {
        composable(DriverRoute.HOME) {
            HomeScreen(
                onOpenNotifications = { navController.navigate(DriverRoute.NOTIFICACIONES) },
                onOpenOrderDetail = { orderId -> navController.navigate(DriverRoute.detalle(orderId)) },
                onOpenHistorial = { navController.navigate(DriverRoute.HISTORIAL) },
                onBlockingStatus = { status ->
                    navController.navigate(DriverRoute.status(status)) { launchSingleTop = true }
                },
            )
        }
        composable(DriverRoute.METRICAS) {
            MetricsScreen(
                onOpenNotifications = { navController.navigate(DriverRoute.NOTIFICACIONES) },
                onOpenHistorial = { navController.navigate(DriverRoute.HISTORIAL) },
            )
        }
        composable(DriverRoute.HISTORIAL) {
            HistorialScreen(
                onBack = { navController.popBackStack() },
                onOpenOrderDetail = { orderId -> navController.navigate(DriverRoute.detalle(orderId)) },
            )
        }
        composable(DriverRoute.AJUSTES) {
            AjustesScreen(
                onOpenNotifications = { navController.navigate(DriverRoute.NOTIFICACIONES) },
            )
        }
        composable(DriverRoute.NOTIFICACIONES) {
            NotificacionesScreen(
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(DriverRoute.AJUSTES) { launchSingleTop = true } },
            )
        }
        composable(
            route = DriverRoute.DETALLE_PATTERN,
            arguments = listOf(navArgument(DriverRoute.DETALLE_ARG) { type = NavType.IntType }),
        ) { backStackEntry ->
            OrderDetailScreen(
                orderId = backStackEntry.arguments?.getInt(DriverRoute.DETALLE_ARG) ?: return@composable,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = DriverRoute.STATUS_PATTERN,
            arguments = listOf(navArgument(DriverRoute.STATUS_ARG) { type = NavType.StringType }),
        ) { entry ->
            val status = runCatching {
                AppStatus.valueOf(entry.arguments?.getString(DriverRoute.STATUS_ARG).orEmpty())
            }.getOrDefault(AppStatus.Healthy)
            val isSession = status == AppStatus.SessionExpired
            AppStatusScreen(
                status = status,
                actionLabel = if (isSession) "Volver a iniciar sesión" else "Abrir ajustes",
                onAction = {
                    // SessionExpired: Slice 1's ForcedLogoutAuthenticator already clears the
                    // session, so DriverAppRoot swaps to the login screen; just unwind here.
                    // Permission gaps: pop back so the re-check on resume can clear the block.
                    if (isSession) onLogout()
                    navController.popBackStack(DriverRoute.HOME, inclusive = false)
                },
            )
        }
        }

        // One persistent nav pill for every tab screen, so switching tabs animates
        // the pill in place instead of re-creating it per screen. It slides out of
        // the way for the non-tab screens (detail, historial, notificaciones…).
        AnimatedVisibility(
            visible = showNavBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(NAV_ANIM_MS)) + slideInVertically(tween(NAV_ANIM_MS)) { it / 2 },
            exit = fadeOut(tween(NAV_ANIM_MS)) + slideOutVertically(tween(NAV_ANIM_MS)) { it / 2 },
        ) {
            CittaBottomNav(
                currentRoute = currentTab.orEmpty(),
                onSelect = selectTab,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }
}
