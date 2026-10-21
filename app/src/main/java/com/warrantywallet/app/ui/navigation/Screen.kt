package com.warrantywallet.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object Products : Screen("products")
    data object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }
    data object AddEditProduct : Screen("add_edit_product?productId={productId}") {
        fun createRoute(productId: String? = null) = if (productId != null) "add_edit_product?productId=$productId" else "add_edit_product"
    }
    data object Reminders : Screen("reminders")
    data object Documents : Screen("documents")
    data object Settings : Screen("settings")
    data object ProSubscription : Screen("pro_subscription")
}

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    data object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home)
    data object Products : BottomNavItem(Screen.Products.route, "Products", Icons.Default.Inventory2)
    data object Documents : BottomNavItem(Screen.Documents.route, "Bills", Icons.Default.ReceiptLong)
    data object Reminders : BottomNavItem(Screen.Reminders.route, "Alerts", Icons.Default.Notifications)
    data object Settings : BottomNavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
}
