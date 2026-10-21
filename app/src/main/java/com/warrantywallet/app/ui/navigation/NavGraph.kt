package com.warrantywallet.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.warrantywallet.app.WarrantyWalletApp
import com.warrantywallet.app.core.model.Category
import com.warrantywallet.app.ui.screens.addedit.AddEditProductScreen
import com.warrantywallet.app.ui.screens.addedit.AddEditProductViewModel
import com.warrantywallet.app.ui.screens.auth.AuthScreen
import com.warrantywallet.app.ui.screens.auth.AuthViewModel
import com.warrantywallet.app.ui.screens.detail.ProductDetailScreen
import com.warrantywallet.app.ui.screens.detail.ProductDetailViewModel
import com.warrantywallet.app.ui.screens.documents.DocumentsScreen
import com.warrantywallet.app.ui.screens.documents.DocumentsViewModel
import com.warrantywallet.app.ui.screens.home.HomeScreen
import com.warrantywallet.app.ui.screens.home.HomeViewModel
import com.warrantywallet.app.ui.screens.onboarding.OnboardingScreen
import com.warrantywallet.app.ui.screens.products.ProductsScreen
import com.warrantywallet.app.ui.screens.products.ProductsViewModel
import com.warrantywallet.app.ui.screens.reminders.RemindersScreen
import com.warrantywallet.app.ui.screens.reminders.RemindersViewModel
import com.warrantywallet.app.ui.screens.search.SearchScreen
import com.warrantywallet.app.ui.screens.search.SearchViewModel
import com.warrantywallet.app.ui.screens.pro.ProSubscriptionScreen
import com.warrantywallet.app.ui.screens.pro.ProSubscriptionViewModel
import com.warrantywallet.app.ui.screens.settings.SettingsScreen
import com.warrantywallet.app.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainAppNavigation(
    app: WarrantyWalletApp,
    navController: NavHostController = rememberNavController()
) {
    val isOnboardingDone by app.preferenceManager.isOnboardingCompleted.collectAsState(initial = false)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Products.route,
        Screen.Documents.route,
        Screen.Reminders.route,
        Screen.Settings.route
    )
    val shouldShowBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar {
                    val bottomItems = listOf(
                        BottomNavItem.Home,
                        BottomNavItem.Products,
                        BottomNavItem.Documents,
                        BottomNavItem.Reminders,
                        BottomNavItem.Settings
                    )
                    bottomItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isOnboardingDone) Screen.Home.route else Screen.Onboarding.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Onboarding Screen
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinishOnboarding = {
                        CoroutineScope(Dispatchers.IO).launch {
                            app.preferenceManager.setOnboardingCompleted(true)
                        }
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // Auth Screen
            composable(Screen.Auth.route) {
                val authVm = rememberAuthViewModel(app)
                AuthScreen(
                    viewModel = authVm,
                    onAuthSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            // Home Dashboard
            composable(Screen.Home.route) {
                val homeVm = rememberHomeViewModel(app)
                HomeScreen(
                    viewModel = homeVm,
                    onNavigateToAddProduct = {
                        navController.navigate(Screen.AddEditProduct.createRoute())
                    },
                    onNavigateToProductDetail = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId))
                    },
                    onNavigateToAllProducts = {
                        navController.navigate(Screen.Products.route)
                    },
                    onNavigateToCategory = { category ->
                        navController.navigate(Screen.Products.route)
                    },
                    onNavigateToSearch = {
                        navController.navigate("search")
                    },
                    onNavigateToProSubscription = {
                        navController.navigate(Screen.ProSubscription.route)
                    }
                )
            }

            // Products Screen
            composable(Screen.Products.route) {
                val productsVm = rememberProductsViewModel(app)
                ProductsScreen(
                    viewModel = productsVm,
                    onNavigateToAddProduct = {
                        navController.navigate(Screen.AddEditProduct.createRoute())
                    },
                    onNavigateToProductDetail = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId))
                    }
                )
            }

            // Add / Edit Product
            composable(
                route = Screen.AddEditProduct.route,
                arguments = listOf(navArgument("productId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                val addEditVm = rememberAddEditViewModel(app)
                AddEditProductScreen(
                    viewModel = addEditVm,
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { savedId ->
                        navController.popBackStack()
                        navController.navigate(Screen.ProductDetail.createRoute(savedId))
                    }
                )
            }

            // Product Detail
            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                val detailVm = rememberDetailViewModel(app)
                ProductDetailScreen(
                    viewModel = detailVm,
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { editId ->
                        navController.navigate(Screen.AddEditProduct.createRoute(editId))
                    }
                )
            }

            // Reminders Screen
            composable(Screen.Reminders.route) {
                val remindersVm = rememberRemindersViewModel(app)
                RemindersScreen(
                    viewModel = remindersVm,
                    onNavigateToProductDetail = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId))
                    }
                )
            }

            // Documents Screen
            composable(Screen.Documents.route) {
                val documentsVm = rememberDocumentsViewModel(app)
                DocumentsScreen(
                    viewModel = documentsVm,
                    onNavigateToProductDetail = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId))
                    }
                )
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                val settingsVm = rememberSettingsViewModel(app)
                SettingsScreen(
                    viewModel = settingsVm,
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route)
                    },
                    onNavigateToProSubscription = {
                        navController.navigate(Screen.ProSubscription.route)
                    }
                )
            }

            // Search Screen
            composable("search") {
                val searchVm = rememberSearchViewModel(app)
                SearchScreen(
                    viewModel = searchVm,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProductDetail = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId))
                    }
                )
            }

            // Pro Subscription Screen
            composable(Screen.ProSubscription.route) {
                val proVm = rememberProSubscriptionViewModel(app)
                ProSubscriptionScreen(
                    viewModel = proVm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// Custom ViewModel Factory Helpers using AppContainer
@Composable
private fun rememberHomeViewModel(app: WarrantyWalletApp): HomeViewModel {
    return viewModel { HomeViewModel(app.productRepository, app.authRepository) }
}

@Composable
private fun rememberProductsViewModel(app: WarrantyWalletApp): ProductsViewModel {
    return viewModel { ProductsViewModel(app.productRepository) }
}

@Composable
private fun rememberAddEditViewModel(app: WarrantyWalletApp): AddEditProductViewModel {
    return viewModel { AddEditProductViewModel(app.productRepository) }
}

@Composable
private fun rememberDetailViewModel(app: WarrantyWalletApp): ProductDetailViewModel {
    return viewModel { ProductDetailViewModel(app.productRepository, app.alarmScheduler) }
}

@Composable
private fun rememberRemindersViewModel(app: WarrantyWalletApp): RemindersViewModel {
    return viewModel { RemindersViewModel(app.database.reminderDao(), app.alarmScheduler) }
}

@Composable
private fun rememberDocumentsViewModel(app: WarrantyWalletApp): DocumentsViewModel {
    return viewModel { DocumentsViewModel(app.database.documentDao()) }
}

@Composable
private fun rememberSettingsViewModel(app: WarrantyWalletApp): SettingsViewModel {
    return viewModel { SettingsViewModel(app.preferenceManager, app.authRepository, app.productRepository) }
}

@Composable
private fun rememberAuthViewModel(app: WarrantyWalletApp): AuthViewModel {
    return viewModel { AuthViewModel(app.authRepository) }
}

@Composable
private fun rememberSearchViewModel(app: WarrantyWalletApp): SearchViewModel {
    return viewModel { SearchViewModel(app.productRepository) }
}

@Composable
private fun rememberProSubscriptionViewModel(app: WarrantyWalletApp): ProSubscriptionViewModel {
    return viewModel { ProSubscriptionViewModel(app.preferenceManager, app.billingManager) }
}
