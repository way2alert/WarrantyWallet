package com.warrantywallet.app.ui.screens.addedit

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.warrantywallet.app.core.designsystem.EmeraldAccent
import com.warrantywallet.app.core.designsystem.components.PrimaryButton
import com.warrantywallet.app.core.model.Category
import com.warrantywallet.app.core.model.ExtractedBillData
import com.warrantywallet.app.core.model.Product
import com.warrantywallet.app.core.util.BillOcrParser
import com.warrantywallet.app.core.util.DateUtils
import com.warrantywallet.app.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.Calendar
import java.util.UUID

data class AddEditUiState(
    val isEditMode: Boolean = false,
    val productId: String = UUID.randomUUID().toString(),
    val name: String = "",
    val brand: String = "",
    val category: Category = Category.ELECTRONICS,
    val purchaseDateMillis: Long? = null,
    val warrantyEndMillis: Long? = null,
    val durationMonths: Int? = 12,
    val seller: String = "",
    val price: String = "",
    val serialNumber: String = "",
    val modelNumber: String = "",
    val notes: String = "",
    val billUri: String? = null,
    val isOcrProcessing: Boolean = false,
    val extractedData: ExtractedBillData? = null,
    val errorMessage: String? = null
)

class AddEditProductViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _saveSuccessEvent = MutableSharedFlow<String>()
    val saveSuccessEvent = _saveSuccessEvent.asSharedFlow()

    fun loadProduct(productId: String?) {
        if (productId == null) return
        viewModelScope.launch {
            val product = productRepository.getProductById(productId)
            if (product != null) {
                _uiState.value = AddEditUiState(
                    isEditMode = true,
                    productId = product.id,
                    name = product.name,
                    brand = product.brand ?: "",
                    category = product.category,
                    purchaseDateMillis = product.purchaseDateMillis,
                    warrantyEndMillis = product.warrantyEndMillis,
                    durationMonths = product.warrantyDurationMonths,
                    seller = product.seller ?: "",
                    price = product.price?.let { "%.2f".format(it) } ?: "",
                    serialNumber = product.serialNumber ?: "",
                    modelNumber = product.modelNumber ?: "",
                    notes = product.notes ?: "",
                    billUri = product.invoiceUri
                )
            }
        }
    }

    fun onBillSelected(context: Context, uri: Uri) {
        _uiState.value = _uiState.value.copy(
            billUri = uri.toString(),
            isOcrProcessing = true
        )

        viewModelScope.launch {
            val extracted = BillOcrParser.parseBillImage(context, uri)
            _uiState.value = _uiState.value.copy(
                isOcrProcessing = false,
                extractedData = extracted,
                name = if (_uiState.value.name.isBlank() && !extracted.productName.isNullOrBlank()) extracted.productName else _uiState.value.name,
                brand = if (_uiState.value.brand.isBlank() && !extracted.brand.isNullOrBlank()) extracted.brand else _uiState.value.brand,
                seller = if (_uiState.value.seller.isBlank() && !extracted.seller.isNullOrBlank()) extracted.seller else _uiState.value.seller,
                purchaseDateMillis = _uiState.value.purchaseDateMillis ?: extracted.purchaseDateMillis,
                price = if (_uiState.value.price.isBlank() && extracted.price != null) "%.2f".format(extracted.price) else _uiState.value.price,
                durationMonths = _uiState.value.durationMonths ?: extracted.warrantyMonthsSuggested ?: 12
            )
            // Auto calculate warranty end if purchase date exists
            _uiState.value.purchaseDateMillis?.let { pDate ->
                _uiState.value.durationMonths?.let { months ->
                    if (_uiState.value.warrantyEndMillis == null) {
                        _uiState.value = _uiState.value.copy(
                            warrantyEndMillis = DateUtils.calculateWarrantyEndDate(pDate, months)
                        )
                    }
                }
            }
        }
    }

    fun onNameChanged(v: String) { _uiState.value = _uiState.value.copy(name = v) }
    fun onBrandChanged(v: String) { _uiState.value = _uiState.value.copy(brand = v) }
    fun onCategoryChanged(c: Category) { _uiState.value = _uiState.value.copy(category = c) }
    fun onSellerChanged(v: String) { _uiState.value = _uiState.value.copy(seller = v) }
    fun onPriceChanged(v: String) { _uiState.value = _uiState.value.copy(price = v) }
    fun onSerialNumberChanged(v: String) { _uiState.value = _uiState.value.copy(serialNumber = v) }
    fun onModelNumberChanged(v: String) { _uiState.value = _uiState.value.copy(modelNumber = v) }
    fun onNotesChanged(v: String) { _uiState.value = _uiState.value.copy(notes = v) }

    fun onPurchaseDateSelected(millis: Long) {
        val months = _uiState.value.durationMonths
        val newEnd = if (months != null && months > 0) {
            DateUtils.calculateWarrantyEndDate(millis, months)
        } else {
            _uiState.value.warrantyEndMillis
        }
        _uiState.value = _uiState.value.copy(
            purchaseDateMillis = millis,
            warrantyEndMillis = newEnd
        )
    }

    fun onDurationSelected(months: Int?) {
        val pDate = _uiState.value.purchaseDateMillis
        val newEnd = if (pDate != null && months != null && months > 0) {
            DateUtils.calculateWarrantyEndDate(pDate, months)
        } else null

        _uiState.value = _uiState.value.copy(
            durationMonths = months,
            warrantyEndMillis = newEnd
        )
    }

    fun onWarrantyEndSelected(millis: Long) {
        _uiState.value = _uiState.value.copy(warrantyEndMillis = millis)
    }

    fun removeBill() {
        _uiState.value = _uiState.value.copy(billUri = null, extractedData = null)
    }

    fun saveProduct() {
        val s = _uiState.value
        if (s.name.isBlank()) {
            _uiState.value = s.copy(errorMessage = "Product name is required.")
            return
        }
        if (s.purchaseDateMillis != null && s.warrantyEndMillis != null && s.warrantyEndMillis < s.purchaseDateMillis) {
            _uiState.value = s.copy(errorMessage = "Warranty end date cannot be earlier than purchase date.")
            return
        }

        val product = Product(
            id = s.productId,
            name = s.name.trim(),
            brand = s.brand.trim().takeIf { it.isNotBlank() },
            category = s.category,
            purchaseDateMillis = s.purchaseDateMillis,
            warrantyStartMillis = s.purchaseDateMillis,
            warrantyEndMillis = s.warrantyEndMillis,
            warrantyDurationMonths = s.durationMonths,
            seller = s.seller.trim().takeIf { it.isNotBlank() },
            price = s.price.toDoubleOrNull(),
            serialNumber = s.serialNumber.trim().takeIf { it.isNotBlank() },
            modelNumber = s.modelNumber.trim().takeIf { it.isNotBlank() },
            invoiceUri = s.billUri,
            notes = s.notes.trim().takeIf { it.isNotBlank() },
            updatedAtMillis = System.currentTimeMillis()
        )

        viewModelScope.launch {
            productRepository.saveProduct(product)
            _saveSuccessEvent.emit(product.id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    viewModel: AddEditProductViewModel,
    productId: String?,
    onNavigateBack: () -> Unit,
    onSaveSuccess: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showSourceSheet by remember { mutableStateOf(productId == null && uiState.billUri == null) }
    var categoryExpanded by remember { mutableStateOf(false) }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            viewModel.onBillSelected(context, tempCameraUri!!)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.onBillSelected(context, uri)
        }
    }

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccessEvent.collect { savedId ->
            onSaveSuccess(savedId)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Warranty" else "Add Warranty", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Bill Attachment & OCR Card
            if (uiState.billUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.billUri!!.endsWith(".pdf", ignoreCase = true)) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444))
                            } else {
                                AsyncImage(
                                    model = uiState.billUri,
                                    contentDescription = "Bill image",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Invoice Attached",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (uiState.isOcrProcessing) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Reading bill with AI...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else if (uiState.extractedData != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Data auto-extracted (%.0f%% confident)".format((uiState.extractedData!!.confidenceScore * 100)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldAccent
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { viewModel.removeBill() }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove bill")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // "Attach Bill" Prompt Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSourceSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scan Bill or Invoice",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Auto-fills product name, dates, store & price",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Product Name
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.onNameChanged(it) },
                label = { Text("Product Name *") },
                placeholder = { Text("e.g. Samsung Galaxy S24, LG Washing Machine") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Brand & Category Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.brand,
                    onValueChange = { viewModel.onBrandChanged(it) },
                    label = { Text("Brand") },
                    placeholder = { Text("e.g. Samsung") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.category.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        Category.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    viewModel.onCategoryChanged(cat)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Purchase Date Picker
            DateFieldBox(
                label = "Purchase Date",
                displayValue = DateUtils.formatDisplayDate(uiState.purchaseDateMillis),
                onClick = {
                    showDatePicker(context, uiState.purchaseDateMillis) { selectedMillis ->
                        viewModel.onPurchaseDateSelected(selectedMillis)
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Warranty Duration Selector Chips
            Text(
                text = "Warranty Duration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("6 Months", 6),
                    Pair("1 Year", 12),
                    Pair("2 Years", 24),
                    Pair("3 Years", 36),
                    Pair("5 Years", 60)
                ).forEach { (label, months) ->
                    FilterChip(
                        selected = uiState.durationMonths == months,
                        onClick = { viewModel.onDurationSelected(if (uiState.durationMonths == months) null else months) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Warranty Expiry Date
            DateFieldBox(
                label = "Warranty Expiry Date",
                displayValue = DateUtils.formatDisplayDate(uiState.warrantyEndMillis),
                onClick = {
                    showDatePicker(context, uiState.warrantyEndMillis) { selectedMillis ->
                        viewModel.onWarrantyEndSelected(selectedMillis)
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Seller & Price Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.seller,
                    onValueChange = { viewModel.onSellerChanged(it) },
                    label = { Text("Seller / Store") },
                    placeholder = { Text("Amazon, Croma") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = uiState.price,
                    onValueChange = { viewModel.onPriceChanged(it) },
                    label = { Text("Price (₹)") },
                    placeholder = { Text("24999") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Serial Number (Optional)
            OutlinedTextField(
                value = uiState.serialNumber,
                onValueChange = { viewModel.onSerialNumberChanged(it) },
                label = { Text("Serial Number / IMEI (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Notes / Service Details
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.onNotesChanged(it) },
                label = { Text("Notes / Service Center (Optional)") },
                placeholder = { Text("Toll-free number, policy number, or contact info") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Save Button
            PrimaryButton(
                text = if (uiState.isEditMode) "Update Warranty" else "Save Warranty",
                onClick = { viewModel.saveProduct() }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Modal Bottom Sheet: Bill Input Source Chooser
    if (showSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSourceSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Add Warranty Bill",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Choose how you'd like to import your purchase document",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                SourceOptionItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Scan Bill (Camera)",
                    subtitle = "Snap a clear photo of your paper receipt",
                    onClick = {
                        showSourceSheet = false
                        val photoFile = File(context.cacheDir, "camera_receipt_${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                        tempCameraUri = uri
                        cameraLauncher.launch(uri)
                    }
                )

                SourceOptionItem(
                    icon = Icons.Default.Image,
                    title = "Choose Photo from Gallery",
                    subtitle = "Pick an invoice photo saved in your phone",
                    onClick = {
                        showSourceSheet = false
                        galleryLauncher.launch("image/*")
                    }
                )

                SourceOptionItem(
                    icon = Icons.Default.PictureAsPdf,
                    title = "Import PDF Document",
                    subtitle = "Attach PDF invoice from Amazon, Flipkart, etc.",
                    onClick = {
                        showSourceSheet = false
                        galleryLauncher.launch("application/pdf")
                    }
                )

                SourceOptionItem(
                    icon = Icons.Default.Edit,
                    title = "Enter Manually",
                    subtitle = "Type warranty details without a bill",
                    onClick = {
                        showSourceSheet = false
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun DateFieldBox(label: String, displayValue: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = displayValue, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SourceOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun showDatePicker(context: Context, initialMillis: Long?, onSelected: (Long) -> Unit) {
    val cal = Calendar.getInstance()
    if (initialMillis != null && initialMillis > 0) {
        cal.timeInMillis = initialMillis
    }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selected = LocalDate.of(year, month + 1, dayOfMonth)
            onSelected(DateUtils.localDateToMillis(selected))
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
