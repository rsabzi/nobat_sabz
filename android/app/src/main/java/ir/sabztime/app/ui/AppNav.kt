package ir.sabztime.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.ui.screens.BookingDetailScreen
import ir.sabztime.app.ui.screens.BookingsScreen
import ir.sabztime.app.ui.screens.BusinessDetailScreen
import ir.sabztime.app.ui.screens.HelpScreen
import ir.sabztime.app.ui.screens.LoginScreen
import ir.sabztime.app.ui.screens.MainScreen
import ir.sabztime.app.ui.screens.MarketerScreen
import ir.sabztime.app.ui.screens.MyBusinessesScreen
import ir.sabztime.app.ui.screens.NotificationsScreen
import ir.sabztime.app.ui.screens.OwnerBusinessScreen
import ir.sabztime.app.ui.screens.ProfileScreen
import ir.sabztime.app.ui.screens.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val MAIN = "main"
    const val BUSINESS = "business/{id}"
    const val BOOKINGS = "bookings"
    const val BOOKING_DETAIL = "booking/{id}"
    const val PROFILE = "profile"
    const val MY_BUSINESSES = "my_businesses"
    const val OWNER = "owner/{businessId}"
    const val MARKETER = "marketer"
    const val NOTIFICATIONS = "notifications"
    const val HELP = "help"

    fun business(id: Int) = "business/$id"
    fun booking(id: Int) = "booking/$id"
    fun owner(businessId: Int) = "owner/$businessId"
}

@Composable
fun AppNav(graph: AppGraph) {
    val navController = rememberNavController()
    val session by graph.session.collectAsState()

    // خروج از همه صفحات هنگام منقضی شدن نشست
    LaunchedEffect(session.loggedIn) {
        if (!session.loggedIn && navController.currentDestination?.route != Routes.SPLASH &&
            navController.currentDestination?.route != Routes.LOGIN
        ) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(graph, navController)
        }
        composable(Routes.LOGIN) {
            LoginScreen(graph, navController)
        }
        composable(Routes.MAIN) {
            MainScreen(graph, navController)
        }
        composable(Routes.PROFILE) {
            ir.sabztime.app.ui.theme.GlassBackground {
                ProfileScreen(graph, navController)
            }
        }
        composable(Routes.BUSINESS) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toIntOrNull() ?: 0
            BusinessDetailScreen(graph, id, navController)
        }
        composable(Routes.BOOKINGS) {
            BookingsScreen(graph, navController)
        }
        composable(Routes.BOOKING_DETAIL) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toIntOrNull() ?: 0
            BookingDetailScreen(graph, id, navController)
        }
        composable(Routes.MY_BUSINESSES) {
            MyBusinessesScreen(graph, navController)
        }
        composable(Routes.OWNER) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("businessId")?.toIntOrNull() ?: 0
            OwnerBusinessScreen(graph, id, navController)
        }
        composable(Routes.MARKETER) {
            MarketerScreen(graph, navController)
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(graph, navController)
        }
        composable(Routes.HELP) {
            HelpScreen(graph, navController)
        }
    }
}

fun NavHostController.navigateOnce(route: String) {
    navigate(route) { launchSingleTop = true }
}
