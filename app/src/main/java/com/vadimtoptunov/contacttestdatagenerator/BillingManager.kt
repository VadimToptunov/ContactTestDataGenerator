package com.vadimtoptunov.contacttestdatagenerator

import android.app.Activity
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manager for Google Play Billing
 * Handles in-app purchases and premium status
 */
class BillingManager(
    private val context: Context,
    private val scope: CoroutineScope
) : PurchasesUpdatedListener, LifecycleObserver {
    
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()
    
    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()
    
    private var billingClient: BillingClient? = null
    
    companion object {
        const val PREMIUM_PRODUCT_ID = "premium_unlock"
        const val FREE_MAX_CONTACTS = 1000
        const val PREMIUM_MAX_CONTACTS = 10000
    }
    
    init {
        initializeBillingClient()
    }
    
    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        
        connectToBilling()
    }
    
    private fun connectToBilling() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Connected successfully
                    queryPurchases()
                }
            }
            
            override fun onBillingServiceDisconnected() {
                // Try to reconnect
                connectToBilling()
            }
        })
    }
    
    /**
     * Query existing purchases to check premium status
     */
    private suspend fun queryPurchasesAsync(): List<Purchase> = suspendCancellableCoroutine { continuation ->
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                continuation.resume(purchases)
            } else {
                continuation.resumeWithException(
                    Exception("Query purchases failed: ${billingResult.debugMessage}")
                )
            }
        } ?: continuation.resumeWithException(Exception("BillingClient is null"))
    }
    
    fun queryPurchases() {
        scope.launch {
            try {
                val purchases = queryPurchasesAsync()
                handlePurchases(purchases)
            } catch (e: Exception) {
                // Log error or handle it
            }
        }
    }
    
    private fun handlePurchases(purchases: List<Purchase>) {
        val hasPremium = purchases.any { purchase ->
            purchase.products.contains(PREMIUM_PRODUCT_ID) &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        
        _isPremium.value = hasPremium
        
        // Acknowledge unacknowledged purchases
        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
        }
    }
    
    private suspend fun acknowledgePurchaseAsync(purchase: Purchase): Boolean = suspendCancellableCoroutine { continuation ->
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                continuation.resume(true)
            } else {
                continuation.resume(false)
            }
        } ?: continuation.resume(false)
    }
    
    private fun acknowledgePurchase(purchase: Purchase) {
        scope.launch {
            try {
                val success = acknowledgePurchaseAsync(purchase)
                if (success) {
                    queryPurchases()
                }
            } catch (e: Exception) {
                // Log error or handle it
            }
        }
    }
    
    /**
     * Query product details and return them
     */
    private suspend fun queryProductDetailsAsync(productId: String): ProductDetails? = suspendCancellableCoroutine { continuation ->
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                productDetailsList.isNotEmpty()
            ) {
                continuation.resume(productDetailsList[0])
            } else {
                continuation.resume(null)
            }
        } ?: continuation.resume(null)
    }
    
    /**
     * Launch purchase flow for premium
     */
    fun launchPurchaseFlow(activity: Activity) {
        scope.launch {
            _purchaseState.value = PurchaseState.Loading
            
            try {
                // Check if billing client is ready
                if (billingClient?.isReady != true) {
                    _purchaseState.value = PurchaseState.Error("Billing not ready, please try again")
                    return@launch
                }
                
                val productDetails = queryProductDetailsAsync(PREMIUM_PRODUCT_ID)
                
                if (productDetails != null) {
                    val productDetailsParamsList = listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                    
                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()
                    
                    // Launch billing flow on Main thread
                    withContext(Dispatchers.Main) {
                        val result = billingClient?.launchBillingFlow(activity, billingFlowParams)
                        if (result?.responseCode != BillingClient.BillingResponseCode.OK) {
                            _purchaseState.value = PurchaseState.Error("Failed to start purchase: ${result?.debugMessage}")
                        }
                    }
                } else {
                    _purchaseState.value = PurchaseState.Error("Product not found")
                }
            } catch (e: Exception) {
                _purchaseState.value = PurchaseState.Error("Failed to load product: ${e.message}")
            }
        }
    }
    
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    handlePurchases(purchases)
                    _purchaseState.value = PurchaseState.Success
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Cancelled
            }
            else -> {
                _purchaseState.value = PurchaseState.Error("Purchase failed")
            }
        }
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        billingClient?.endConnection()
    }
    
    sealed class PurchaseState {
        object Idle : PurchaseState()
        object Loading : PurchaseState()
        object Success : PurchaseState()
        object Cancelled : PurchaseState()
        data class Error(val message: String) : PurchaseState()
    }
}

