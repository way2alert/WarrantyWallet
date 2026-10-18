package com.warrantywallet.app.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrantywallet.app.core.datastore.PreferenceManager
import com.warrantywallet.app.core.designsystem.EmeraldAccent
import com.warrantywallet.app.core.designsystem.components.ConfirmDialog
import com.warrantywallet.app.core.model.AppLanguage
import com.warrantywallet.app.core.model.AppThemeMode
import com.warrantywallet.app.core.model.Product
import com.warrantywallet.app.core.util.DateUtils
import com.warrantywallet.app.core.util.ExportUtils
import com.warrantywallet.app.data.repository.AuthRepository
import com.warrantywallet.app.data.repository.ProductRepository
import com.warrantywallet.app.data.repository.UserSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val userSession: UserSession = UserSession(),
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val lastSyncMillis: Long? = null,
    val isSyncing: Boolean = false
)

class SettingsViewModel(
    private val preferenceManager: PreferenceManager,
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent = _snackbarEvent.asSharedFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        authRepository.userSession,
        preferenceManager.themeMode,
        preferenceManager.language,
        preferenceManager.lastSyncMillis,
        _isSyncing
    ) { session, theme, lang, lastSync, syncing ->
        SettingsUiState(
            userSession = session,
            themeMode = theme,
            language = lang,
            lastSyncMillis = lastSync,
            isSyncing = syncing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setTheme(theme: AppThemeMode) {
        viewModelScope.launch { preferenceManager.setThemeMode(theme) }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch { preferenceManager.setLanguage(lang) }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = productRepository.syncAllWithCloud()
            _isSyncing.value = false
            if (result.isSuccess) {
                _snackbarEvent.emit("Cloud sync complete (${result.getOrDefault(0)} items updated)")
            } else {
                _snackbarEvent.emit(result.exceptionOrNull()?.localizedMessage ?: "Sync failed. Sign in required.")
            }
        }
    }

    fun exportData(context: Context, format: String) {
        viewModelScope.launch {
            val products = productRepository.getAllProductsFlow().firstOrNull() ?: emptyList()
            if (products.isEmpty()) {
                _snackbarEvent.emit("No products to export")
                return@launch
            }
            if (format == "JSON") {
                val json = ExportUtils.exportToJson(products)
                ExportUtils.shareExportFile(context, json, "WarrantyWallet_Export.json", "application/json")
            } else {
                val csv = ExportUtils.exportToCsv(products)
                ExportUtils.shareExportFile(context, csv, "WarrantyWallet_Export.csv", "text/csv")
            }
        }
    }

    fun clearLocalData() {
        viewModelScope.launch {
            productRepository.clearLocalData()
            _snackbarEvent.emit("Local cache cleared successfully")
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _snackbarEvent.emit("Signed out")
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val result = authRepository.deleteAccount()
            if (result.isSuccess) {
                productRepository.clearLocalData()
                _snackbarEvent.emit("Account deleted successfully")
            } else {
                _snackbarEvent.emit(result.exceptionOrNull()?.localizedMessage ?: "Account deletion failed")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAuth: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLangDialog by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) })
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
            // Account & Sync Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Account & Cloud Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.userSession.isSignedIn) uiState.userSession.email ?: "Signed In" else "Guest Mode (Offline)",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (uiState.userSession.isSignedIn) "Cloud sync active" else "Stored locally on this phone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!uiState.userSession.isSignedIn) {
                            Button(onClick = onNavigateToAuth, shape = RoundedCornerShape(10.dp)) {
                                Text("Sign In")
                            }
                        } else {
                            IconButton(onClick = { viewModel.signOut() }) {
                                Icon(Icons.Default.Logout, contentDescription = "Sign out", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    if (uiState.userSession.isSignedIn) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Last Synced", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (uiState.lastSyncMillis != null) DateUtils.formatDisplayDate(uiState.lastSyncMillis) else "Never",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                OutlinedButton(onClick = { viewModel.triggerSync() }) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Now")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preferences Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    SettingsRowItem(
                        icon = Icons.Default.DarkMode,
                        title = "App Theme",
                        subtitle = when (uiState.themeMode) {
                            AppThemeMode.SYSTEM -> "System Default"
                            AppThemeMode.LIGHT -> "Light"
                            AppThemeMode.DARK -> "Dark"
                        },
                        onClick = { showThemeDialog = true }
                    )

                    Divider()

                    SettingsRowItem(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = uiState.language.displayName,
                        onClick = { showLangDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data Portability & Privacy Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Data Portability & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    SettingsRowItem(
                        icon = Icons.Default.FileDownload,
                        title = "Export Data (JSON)",
                        subtitle = "Download all your warranty records in JSON format",
                        onClick = { viewModel.exportData(context, "JSON") }
                    )

                    Divider()

                    SettingsRowItem(
                        icon = Icons.Default.FileDownload,
                        title = "Export Data (CSV / Excel)",
                        subtitle = "Export readable spreadsheet table",
                        onClick = { viewModel.exportData(context, "CSV") }
                    )

                    Divider()

                    SettingsRowItem(
                        icon = Icons.Default.DeleteForever,
                        title = "Clear Local Data",
                        subtitle = "Remove locally stored warranties from this device",
                        onClick = { showClearDataConfirm = true },
                        textColor = MaterialTheme.colorScheme.error
                    )

                    if (uiState.userSession.isSignedIn) {
                        Divider()
                        SettingsRowItem(
                            icon = Icons.Default.DeleteForever,
                            title = "Delete Account",
                            subtitle = "Permanently remove your account and all cloud data",
                            onClick = { showDeleteAccountConfirm = true },
                            textColor = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legal & About Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "About & Legal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    SettingsRowItem(
                        icon = Icons.Default.Lock,
                        title = "Privacy Policy",
                        subtitle = "How we protect and manage your data",
                        onClick = { showPrivacyPolicy = true }
                    )

                    Divider()

                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = "Terms of Service",
                        subtitle = "App terms and conditions",
                        onClick = { showTerms = true }
                    )

                    Divider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("App Version", style = MaterialTheme.typography.bodyMedium)
                        Text("1.0.0 (Production Build)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Theme Chooser Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    listOf(
                        Pair(AppThemeMode.SYSTEM, "System Default"),
                        Pair(AppThemeMode.LIGHT, "Light"),
                        Pair(AppThemeMode.DARK, "Dark")
                    ).forEach { (mode, title) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTheme(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = uiState.themeMode == mode, onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(title)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Language Chooser Dialog
    if (showLangDialog) {
        AlertDialog(
            onDismissRequest = { showLangDialog = false },
            title = { Text("Select Language") },
            text = {
                Column {
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(lang)
                                    showLangDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = uiState.language == lang, onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(lang.displayName)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLangDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearDataConfirm) {
        ConfirmDialog(
            title = "Clear Local Data?",
            message = "This will remove all locally stored warranty records from this device. Cloud data will remain safe if you are signed in.",
            confirmButtonText = "Clear Data",
            onConfirm = {
                showClearDataConfirm = false
                viewModel.clearLocalData()
            },
            onDismiss = { showClearDataConfirm = false }
        )
    }

    if (showDeleteAccountConfirm) {
        ConfirmDialog(
            title = "Delete Account?",
            message = "Are you sure you want to permanently delete your account and all associated cloud backups? This cannot be undone.",
            confirmButtonText = "Delete Account",
            onConfirm = {
                showDeleteAccountConfirm = false
                viewModel.deleteAccount()
            },
            onDismiss = { showDeleteAccountConfirm = false }
        )
    }

    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = """
                            Warranty Wallet Privacy Commitment:
                            
                            1. On-Device OCR: Bill text extraction occurs directly on your device using on-device machine learning models. Your invoice images are not shared with third-party advertising brokers.
                            
                            2. Cloud Storage: Only if you explicitly choose to sign in, your warranty metadata is backed up to secure Cloud Firestore, and invoice documents are stored in private Firebase Cloud Storage.
                            
                            3. Data Rights: You have the absolute right to export all your records (JSON/CSV) at any time and permanently delete your account and documents directly within the app.
                            
                            4. Zero Ad Tracking: We do not run advertising SDKs or monetize your personal purchase data.
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicy = false }) { Text("Close") }
            }
        )
    }

    if (showTerms) {
        AlertDialog(
            onDismissRequest = { showTerms = false },
            title = { Text("Terms of Service", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = """
                            Warranty Wallet Terms of Service:
                            
                            1. Purpose: Warranty Wallet is a digital organizer to help you keep track of your product invoices and warranty periods.
                            
                            2. Accuracy: Date suggestions are extracted to assist you. Always verify extracted invoice numbers, dates, and amounts against your original purchase bill.
                            
                            3. Manufacturer Warranties: Warranty terms, coverage, and authorized repairs are determined exclusively by the respective product manufacturer or retailer.
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTerms = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    textColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (textColor != Color.Unspecified) textColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = textColor)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
    }
}
