package com.warrantywallet.app.ui.screens.products

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrantywallet.app.core.designsystem.components.EmptyStateView
import com.warrantywallet.app.core.designsystem.components.StatusPill
import com.warrantywallet.app.core.designsystem.components.WarrantyProductCard
import com.warrantywallet.app.core.model.Category
import com.warrantywallet.app.core.model.Product
import com.warrantywallet.app.core.model.WarrantyStatus
import com.warrantywallet.app.core.util.DateUtils
import com.warrantywallet.app.data.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class ProductSortOption(val displayName: String) {
    RECENT("Newest First"),
    EXPIRING_SOONEST("Expiring Soonest"),
    NAME("Name (A-Z)"),
    PRICE("Price (High to Low)")
}

data class ProductsUiState(
    val searchQuery: String = "",
    val selectedStatusFilter: WarrantyStatus? = null,
    val selectedCategoryFilter: Category? = null,
    val sortOption: ProductSortOption = ProductSortOption.RECENT,
    val isGridView: Boolean = false,
    val filteredProducts: List<Product> = emptyList(),
    val totalCount: Int = 0
)

private data class FilterCriteria(
    val query: String,
    val status: WarrantyStatus?,
    val category: Category?,
    val sort: ProductSortOption,
    val isGrid: Boolean
)

class ProductsViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow<WarrantyStatus?>(null)
    private val _categoryFilter = MutableStateFlow<Category?>(null)
    private val _sortOption = MutableStateFlow(ProductSortOption.RECENT)
    private val _isGridView = MutableStateFlow(false)

    private val filterCriteriaFlow: Flow<FilterCriteria> = combine(
        _searchQuery,
        _statusFilter,
        _categoryFilter,
        combine(_sortOption, _isGridView) { sort, isGrid -> Pair(sort, isGrid) }
    ) { query, status, category, (sort, isGrid) ->
        FilterCriteria(query, status, category, sort, isGrid)
    }

    val uiState: StateFlow<ProductsUiState> = combine(
        productRepository.getAllProductsFlow(),
        filterCriteriaFlow
    ) { products, criteria ->
        val now = System.currentTimeMillis()

        val filtered = products.filter { p ->
            // Search filter
            val matchesQuery = criteria.query.isBlank() ||
                    p.name.contains(criteria.query, ignoreCase = true) ||
                    (p.brand?.contains(criteria.query, ignoreCase = true) == true) ||
                    (p.seller?.contains(criteria.query, ignoreCase = true) == true) ||
                    (p.modelNumber?.contains(criteria.query, ignoreCase = true) == true)

            // Status filter
            val matchesStatus = criteria.status == null || DateUtils.getWarrantyStatus(p.warrantyEndMillis, now) == criteria.status

            // Category filter
            val matchesCategory = criteria.category == null || p.category == criteria.category

            matchesQuery && matchesStatus && matchesCategory
        }.let { list ->
            when (criteria.sort) {
                ProductSortOption.RECENT -> list.sortedByDescending { it.createdAtMillis }
                ProductSortOption.EXPIRING_SOONEST -> list.sortedWith(
                    compareBy(nullsLast()) { it.warrantyEndMillis }
                )
                ProductSortOption.NAME -> list.sortedBy { it.name.lowercase() }
                ProductSortOption.PRICE -> list.sortedByDescending { it.price ?: 0.0 }
            }
        }

        ProductsUiState(
            searchQuery = criteria.query,
            selectedStatusFilter = criteria.status,
            selectedCategoryFilter = criteria.category,
            sortOption = criteria.sort,
            isGridView = criteria.isGrid,
            filteredProducts = filtered,
            totalCount = products.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductsUiState()
    )

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun onStatusFilterChanged(status: WarrantyStatus?) { _statusFilter.value = status }
    fun onCategoryFilterChanged(category: Category?) { _categoryFilter.value = category }
    fun onSortOptionChanged(sort: ProductSortOption) { _sortOption.value = sort }
    fun toggleGridView() { _isGridView.value = !_isGridView.value }
    fun clearAllFilters() {
        _searchQuery.value = ""
        _statusFilter.value = null
        _categoryFilter.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: ProductsViewModel,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToProductDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Products (${uiState.filteredProducts.size})",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    IconButton(onClick = { viewModel.toggleGridView() }) {
                        Icon(
                            imageVector = if (uiState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View"
                        )
                    }
                }
            )
        },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search input field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search products, brands, stores...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Status filter chips strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedStatusFilter == null,
                    onClick = { viewModel.onStatusFilterChanged(null) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = uiState.selectedStatusFilter == WarrantyStatus.ACTIVE,
                    onClick = { viewModel.onStatusFilterChanged(if (uiState.selectedStatusFilter == WarrantyStatus.ACTIVE) null else WarrantyStatus.ACTIVE) },
                    label = { Text("Active") }
                )
                FilterChip(
                    selected = uiState.selectedStatusFilter == WarrantyStatus.EXPIRING_SOON,
                    onClick = { viewModel.onStatusFilterChanged(if (uiState.selectedStatusFilter == WarrantyStatus.EXPIRING_SOON) null else WarrantyStatus.EXPIRING_SOON) },
                    label = { Text("Expiring Soon") }
                )
                FilterChip(
                    selected = uiState.selectedStatusFilter == WarrantyStatus.EXPIRED,
                    onClick = { viewModel.onStatusFilterChanged(if (uiState.selectedStatusFilter == WarrantyStatus.EXPIRED) null else WarrantyStatus.EXPIRED) },
                    label = { Text("Expired") }
                )
                if (uiState.selectedCategoryFilter != null) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.onCategoryFilterChanged(null) },
                        label = { Text("${uiState.selectedCategoryFilter?.displayName} ✕") }
                    )
                }
            }

            // Products List or Grid
            if (uiState.filteredProducts.isEmpty()) {
                EmptyStateView(
                    title = "No products found",
                    description = if (uiState.searchQuery.isNotBlank() || uiState.selectedStatusFilter != null || uiState.selectedCategoryFilter != null)
                        "Try adjusting your filters or search terms."
                    else
                        "You haven't saved any warranties yet.",
                    buttonText = if (uiState.searchQuery.isNotBlank() || uiState.selectedStatusFilter != null || uiState.selectedCategoryFilter != null)
                        "Clear Filters"
                    else
                        "Add Warranty",
                    onButtonClick = {
                        if (uiState.searchQuery.isNotBlank() || uiState.selectedStatusFilter != null || uiState.selectedCategoryFilter != null) {
                            viewModel.clearAllFilters()
                        } else {
                            onNavigateToAddProduct()
                        }
                    },
                    modifier = Modifier.padding(top = 40.dp)
                )
            } else if (uiState.isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredProducts) { product ->
                        ProductGridItem(
                            product = product,
                            onClick = { onNavigateToProductDetail(product.id) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredProducts) { product ->
                        WarrantyProductCard(
                            product = product,
                            onClick = { onNavigateToProductDetail(product.id) }
                        )
                    }
                }
            }
        }
    }

    // Filter & Sort Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Sort & Filter",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Sort By", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                ProductSortOption.entries.forEach { option ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.onSortOptionChanged(option)
                                showFilterSheet = false
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = option.displayName)
                            if (uiState.sortOption == option) {
                                Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Filter by Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Category.entries.forEach { cat ->
                        FilterChip(
                            selected = uiState.selectedCategoryFilter == cat,
                            onClick = {
                                viewModel.onCategoryFilterChanged(if (uiState.selectedCategoryFilter == cat) null else cat)
                                showFilterSheet = false
                            },
                            label = { Text(cat.displayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ProductGridItem(product: Product, onClick: () -> Unit) {
    val status = DateUtils.getWarrantyStatus(product.warrantyEndMillis)
    val remainingText = DateUtils.formatRemainingText(product.warrantyEndMillis)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(status = status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )

            if (!product.brand.isNullOrBlank()) {
                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = remainingText,
                style = MaterialTheme.typography.labelSmall,
                color = Color(status.badgeColor),
                fontWeight = FontWeight.Medium
            )

            if (product.price != null && product.price > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${"%,.0f".format(product.price)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
