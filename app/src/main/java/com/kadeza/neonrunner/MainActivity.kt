package com.kadeza.neonrunner

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.android.billingclient.api.*

class MainActivity : Activity(), PurchasesUpdatedListener {
    private lateinit var game: RunnerView
    private lateinit var billing: BillingClient
    private val products = listOf("coins_500", "coins_1500", "coins_5000", "coins_12000")
    private var productDetails = mutableMapOf<String, ProductDetails>()
    private var pendingCoins = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        game = RunnerView(this) { action, amount ->
            when (action) {
                "buy" -> buyCoins(amount)
                "menu" -> game.showMenu()
            }
        }
        setContentView(game)
        billing = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        connectBilling()
    }

    private fun connectBilling() {
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) queryProducts()
            }
            override fun onBillingServiceDisconnected() { /* reconnect when app is resumed */ }
        })
    }

    private fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            products.map { id -> QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(BillingClient.ProductType.INAPP).build() }
        ).build()
        billing.queryProductDetailsAsync(params) { result, response ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails.clear()
                response.productDetailsList.forEach { productDetails[it.productId] = it }
            }
        }
    }

    private fun buyCoins(index: Int) {
        if (!billing.isReady) {
            Toast.makeText(this, "Google Play Billing সংযোগ হচ্ছে—একটু পরে চেষ্টা করুন", Toast.LENGTH_SHORT).show()
            connectBilling(); return
        }
        val ids = products
        val id = ids.getOrNull(index) ?: return
        val details = productDetails[id]
        if (details == null) {
            Toast.makeText(this, "প্যাকেজ এখনো লোড হয়নি। Play Console-এ product ID তৈরি করে আবার চেষ্টা করুন।", Toast.LENGTH_LONG).show()
            queryProducts(); return
        }
        pendingCoins = when (id) { "coins_500" -> 500; "coins_1500" -> 1500; "coins_5000" -> 5000; else -> 12000 }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()
        billing.launchBillingFlow(this, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams)).build())
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    // Prototype note: production apps should verify purchase tokens on a secure backend before granting items.
                    if (!purchase.isAcknowledged) {
                        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                        billing.acknowledgePurchase(params) { ack ->
                            if (ack.responseCode == BillingClient.BillingResponseCode.OK) runOnUiThread { grantPurchasedCoins(purchase.products) }
                        }
                    } else grantPurchasedCoins(purchase.products)
                }
            }
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(this, "Purchase সম্পন্ন হয়নি: ${result.debugMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun grantPurchasedCoins(ids: List<String>) {
        val amount = when (ids.firstOrNull()) { "coins_500" -> 500; "coins_1500" -> 1500; "coins_5000" -> 5000; "coins_12000" -> 12000; else -> 0 }
        if (amount > 0) {
            game.addCoins(amount)
            Toast.makeText(this, "+$amount coins যোগ হয়েছে", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() { super.onResume(); if (::billing.isInitialized && !billing.isReady) connectBilling() }
    override fun onDestroy() { if (::billing.isInitialized) billing.endConnection(); super.onDestroy() }
}
