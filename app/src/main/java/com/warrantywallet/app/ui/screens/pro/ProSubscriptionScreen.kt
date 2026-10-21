package com.warrantywallet.app.ui.screens.pro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrantywallet.app.core.datastore.PreferenceManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProSubscriptionViewModel(
    private val preferenceManager: PreferenceManager,
    private val billingManager: com.warrantywallet.app.core.billing.BillingManager
) : ViewModel() {

    val isProUser: StateFlow<Boolean> = preferenceManager.isProUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun startSubscriptionFlow(activity: android.app.Activity?, isAnnual: Boolean, onError: (String) -> Unit) {
        if (activity == null) {
            onError("Unable to launch checkout. Please restart the app.")
            return
        }
        billingManager.launchSubscription(activity, isAnnual, onError)
    }

    fun applyPromoCode(code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            // Production validation: All bypass codes removed
            onResult(false, "No active promotional campaign found for '$code'.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProSubscriptionScreen(
    viewModel: ProSubscriptionViewModel,
    onNavigateBack: () -> Unit
) {
    val isProUser by viewModel.isProUser.collectAsState()
    var selectedPlanAnnual by remember { mutableStateOf(true) }
    var showPromoDialog by remember { mutableStateOf(false) }
    var promoCode by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    val goldColor = Color(0xFFD97706)
    val goldLight = Color(0xFFFEF3C7)
    val navyDark = Color(0xFF0F172A)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = navyDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(navyDark)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Crown Badge
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Warranty Wallet PRO",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = if (isProUser) "You are an active PRO Member ✨" else "Never lose a bill or miss free repairs again.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Pro Features List Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProFeatureRow(
                        icon = Icons.Default.AllInclusive,
                        title = "Unlimited Warranties & Invoices",
                        subtitle = "Free tier allows 10 items. PRO lets you store everything you buy."
                    )
                    ProFeatureRow(
                        icon = Icons.Default.CloudDone,
                        title = "Encrypted Cloud Vault (PDF & HD Images)",
                        subtitle = "Never lose faded thermal paper bills. Safe forever in the cloud."
                    )
                    ProFeatureRow(
                        icon = Icons.Default.FamilyRestroom,
                        title = "Family & Household Sharing",
                        subtitle = "Sync appliances and receipts across all family members' phones."
                    )
                    ProFeatureRow(
                        icon = Icons.Default.PictureAsPdf,
                        title = "Certified Resale & Insurance Report",
                        subtitle = "1-click export of verified bills for higher resale value on OLX & Cashify."
                    )
                    ProFeatureRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "Priority Multi-Channel Alerts",
                        subtitle = "Get alerted 60, 30, 7 and 1 day before warranty expires to claim free service."
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isProUser) {
                // Plan Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Annual Plan (Recommended)
                    PlanCard(
                        modifier = Modifier.weight(1f),
                        title = "Annual Plan",
                        price = "₹199",
                        period = "/ year",
                        badge = "BEST VALUE (50% OFF)",
                        isSelected = selectedPlanAnnual,
                        onClick = { selectedPlanAnnual = true }
                    )

                    // Monthly Plan
                    PlanCard(
                        modifier = Modifier.weight(1f),
                        title = "Monthly Plan",
                        price = "₹29",
                        period = "/ month",
                        badge = "FLEXIBLE",
                        isSelected = !selectedPlanAnnual,
                        onClick = { selectedPlanAnnual = false }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Upgrade Button
                Button(
                    onClick = {
                        viewModel.startSubscriptionFlow(activity, selectedPlanAnnual) { errorMsg ->
                            scope.launch {
                                snackbarHostState.showSnackbar(errorMsg)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = goldColor,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedPlanAnnual) "Get PRO for ₹199 / Year" else "Get PRO for ₹29 / Month",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Promo Code Redeem
                TextButton(onClick = { showPromoDialog = !showPromoDialog }) {
                    Text(
                        text = "Have a promo or activation code?",
                        color = Color(0xFF38BDF8),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                AnimatedVisibility(visible = showPromoDialog) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = promoCode,
                            onValueChange = { promoCode = it },
                            label = { Text("Enter Promo Code", color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.applyPromoCode(promoCode) { success, msg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                    if (success) showPromoDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                        ) {
                            Text("Apply Code")
                        }
                    }
                }
            } else {
                // Active Pro Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF065F46),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("PRO Membership Active", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("All premium features & unlimited storage unlocked.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFA7F3D0))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF334155)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun PlanCard(
    modifier: Modifier = Modifier,
    title: String,
    price: String,
    period: String,
    badge: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155)
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.Black else Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF94A3B8)
            )

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = period,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                )
            }
        }
    }
}
