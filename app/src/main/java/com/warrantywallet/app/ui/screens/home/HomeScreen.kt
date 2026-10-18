package com.warrantywallet.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrantywallet.app.core.designsystem.EmeraldAccent
import com.warrantywallet.app.core.designsystem.components.EmptyStateView
import com.warrantywallet.app.core.designsystem.components.StatCard
import com.warrantywallet.app.core.designsystem.components.WarrantyProductCard
import com.warrantywallet.app.core.model.Category
import com.warrantywallet.app.core.model.Product
import com.warrantywallet.app.core.model.WarrantyStatus
import com.warrantywallet.app.core.util.DateUtils
import com.warrantywallet.app.data.repository.AuthRepository
import com.warrantywallet.app.data.repository.ProductRepository
import com.warrantywallet.app.data.repository.UserSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val userSession: UserSession = UserSession(),
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val expiringCount: Int = 0,
    val expiredCount: Int = 0,
    val totalProtectedValue: Double = 0.0,
    val expiringSoonProducts: List<Product> = emptyList(),
    val recentProducts: List<Product> = emptyList()
)

class HomeViewModel(
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        authRepository.userSession,
        productRepository.getAllProductsFlow()
    ) { session, products ->
        val now = System.currentTimeMillis()
        var active = 0
        var expiring = 0
        var expired = 0
        var totalValue = 0.0

        for (p in products) {
            when (DateUtils.getWarrantyStatus(p.warrantyEndMillis, now)) {
                WarrantyStatus.ACTIVE -> active++
                WarrantyStatus.EXPIRING_SOON -> expiring++
                WarrantyStatus.EXPIRED -> expired++
                WarrantyStatus.UNKNOWN -> {}
            }
            if (p.price != null && p.price > 0) {
                totalValue += p.price
            }
        }

        val expiringList = products.filter {
            DateUtils.getWarrantyStatus(it.warrantyEndMillis, now) == WarrantyStatus.EXPIRING_SOON
        }.sortedBy { it.warrantyEndMillis }

        HomeUiState(
            userSession = session,
            totalCount = products.size,
            activeCount = active,
            expiringCount = expiring,
            expiredCount = expired,
            totalProtectedValue = totalValue,
            expiringSoonProducts = expiringList,
            recentProducts = products.take(6)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToProductDetail: (String) -> Unit,
    onNavigateToAllProducts: () -> Unit,
    onNavigateToCategory: (Category) -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddProduct,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Warranty")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Header Bar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Warranty Wallet",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldAccent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.userSession.email != null) "Hi, ${uiState.userSession.email?.substringBefore('@')}" else "Hello, Warranty Saver 👋",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Search entry button
                        IconButtonWithBackground(
                            icon = Icons.Default.Search,
                            onClick = onNavigateToSearch
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Protected Value Banner
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Protected Assets Value",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "₹${"%,.0f".format(uiState.totalProtectedValue)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldAccent.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Stat Cards Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Active",
                        value = "${uiState.activeCount}",
                        icon = Icons.Default.CheckCircle,
                        iconTint = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAllProducts
                    )
                    StatCard(
                        title = "Expiring",
                        value = "${uiState.expiringCount}",
                        icon = Icons.Default.Warning,
                        iconTint = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAllProducts
                    )
                    StatCard(
                        title = "Expired",
                        value = "${uiState.expiredCount}",
                        icon = Icons.Default.Error,
                        iconTint = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAllProducts
                    )
                }
            }

            // Expiring Soon Alert Section
            if (uiState.expiringSoonProducts.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Expiring Soon (< 30 Days)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(onClick = onNavigateToAllProducts) {
                                Text("View All")
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                items(uiState.expiringSoonProducts.take(3)) { product ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        WarrantyProductCard(
                            product = product,
                            onClick = { onNavigateToProductDetail(product.id) }
                        )
                    }
                }
            }

            // Categories horizontal chip strip
            item {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Category.entries.take(6).forEach { category ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { onNavigateToCategory(category) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = category.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recently Added Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Added",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.recentProducts.isNotEmpty()) {
                        TextButton(onClick = onNavigateToAllProducts) {
                            Text("See All")
                        }
                    }
                }
            }

            if (uiState.recentProducts.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No warranties saved yet",
                        description = "Tap the '+' button below to scan your first receipt or invoice in seconds!",
                        buttonText = "Add Your First Warranty",
                        onButtonClick = onNavigateToAddProduct,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                items(uiState.recentProducts) { product ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        WarrantyProductCard(
                            product = product,
                            onClick = { onNavigateToProductDetail(product.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IconButtonWithBackground(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
