package com.warrantywallet.app.core.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.warrantywallet.app.core.datastore.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Production Billing & Payment Manager for Warranty Wallet PRO.
 * Handles official Google Play Billing In-App Subscriptions
 * and fallback UPI intent for Indian users.
 */
class BillingManager(
    private val context: Context,
    private val preferenceManager: PreferenceManager,
    private val coroutineScope: CoroutineScope
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_PRO_ANNUAL = "pro_annual_199"
        const val PRODUCT_PRO_MONTHLY = "pro_monthly_29"
        const val UPI_MERCHANT_VPA = "way2alert@upi"
        const val UPI_MERCHANT_NAME = "Warranty Wallet"
    }

    private var billingClient: BillingClient? = null
    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady: StateFlow<Boolean> = _isBillingReady.asStateFlow()

    private val _productDetailsList = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsList: StateFlow<Map<String, ProductDetails>> = _productDetailsList.asStateFlow()

    fun initialize() {
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()

        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(pendingPurchasesParams)
            .build()

        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isBillingReady.value = true
                    querySubscriptions()
                    restoreActivePurchases()
                } else {
                    _isBillingReady.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                _isBillingReady.value = false
            }
        })
    }

    private fun querySubscriptions() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PRO_ANNUAL)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PRO_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = productDetailsList.associateBy { it.productId }
                _productDetailsList.value = map
            }
        }
    }

    fun launchSubscription(activity: Activity, isAnnual: Boolean, onError: (String) -> Unit) {
        val productId = if (isAnnual) PRODUCT_PRO_ANNUAL else PRODUCT_PRO_MONTHLY
        val productDetails = _productDetailsList.value[productId]

        if (productDetails != null) {
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken != null) {
                val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()

                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))
                    .build()

                billingClient?.launchBillingFlow(activity, flowParams)
                return
            }
        }

        // Fallback: If running outside Play Console subscription track, launch Native UPI Checkout
        launchUpiCheckout(activity, isAnnual, onError)
    }

    /**
     * Native UPI intent launcher (GPay, PhonePe, Paytm, BHIM)
     */
    fun launchUpiCheckout(activity: Activity, isAnnual: Boolean, onError: (String) -> Unit) {
        val amount = if (isAnnual) "199.00" else "29.00"
        val planName = if (isAnnual) "Warranty Wallet PRO 1 Year" else "Warranty Wallet PRO 1 Month"

        val uri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", UPI_MERCHANT_VPA)
            .appendQueryParameter("pn", UPI_MERCHANT_NAME)
            .appendQueryParameter("tn", planName)
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .build()

        val upiIntent = Intent(Intent.ACTION_VIEW, uri)
        try {
            activity.startActivity(Intent.createChooser(upiIntent, "Pay ₹$amount using UPI"))
        } catch (e: Exception) {
            onError("No UPI payment app (Google Pay / PhonePe / Paytm) found on device.")
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            coroutineScope.launch {
                val isAnnual = purchase.products.contains(PRODUCT_PRO_ANNUAL)
                val durationDays = if (isAnnual) 365L else 30L
                val expiry = System.currentTimeMillis() + (durationDays * 24 * 60 * 60 * 1000)

                preferenceManager.setProUser(true, expiry)

                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { }
                }
            }
        }
    }

    fun restoreActivePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val activePurchase = purchases.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (activePurchase != null) {
                    handlePurchase(activePurchase)
                }
            }
        }
    }
}
