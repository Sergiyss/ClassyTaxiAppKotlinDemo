/*
 * Copyright 2018 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dev.billingwebnauts.data

import android.util.Log
import com.android.billingclient.api.Purchase
import com.dev.billingwebnauts.Constants
import com.dev.billingwebnauts.data.disk.BillingLocalDataSource
import com.dev.billingwebnauts.data.network.BillingRemoteDataSource
import com.dev.billingwebnauts.data.otps.OneTimeProductPurchaseStatus
import com.dev.billingwebnauts.data.subscriptions.SubscriptionStatus
import com.dev.billingwebnauts.gpbl.BillingClientLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Repository handling the work with subscriptions.
 */
class BillingRepository private constructor(
    private val localDataSource: BillingLocalDataSource,
    private val remoteDataSource: BillingRemoteDataSource,
    private val billingClientLifecycle: BillingClientLifecycle,
    private val externalScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    /**
     * True when there are pending network requests.
     */
    val loading: StateFlow<Boolean> = remoteDataSource.loading

    /**
     * [MutableStateFlow] to coordinate updates from the database and the network.
     * Intended to be collected by ViewModel
     */
    val subscriptions: StateFlow<List<SubscriptionStatus>> =
        localDataSource.getSubscriptions()
            .stateIn(externalScope, SharingStarted.WhileSubscribed(), emptyList())

    /**
     * [MutableStateFlow] to coordinate updates from the database and the network.
     * Intended to be collected by ViewModel
     */
    val oneTimeProductPurchases: StateFlow<List<OneTimeProductPurchaseStatus>> =
        localDataSource.getOneTimeProducts()
            .stateIn(externalScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _basicContent = MutableStateFlow<ContentResource?>(null)
    private val _premiumContent = MutableStateFlow<ContentResource?>(null)
    private val _otpContent = MutableStateFlow<ContentResource?>(null)

    /**
     * [StateFlow] with the basic content.
     * Intended to be collected by ViewModel
     */
    val basicContent = _basicContent.asStateFlow()

    /**
     * [StateFlow] with the premium content.
     * Intended to be collected by ViewModel
     */
    val premiumContent = _premiumContent.asStateFlow()

    /**
     * [StateFlow] with the one-time product content.
     * Intended to be collected by ViewModel
     */
    val otpContent = _otpContent.asStateFlow()

    // Set to true when a returned purchases is an auto-renewing basic subscription.
    val hasRenewableBasic: StateFlow<Boolean> =
        billingClientLifecycle.subscriptionPurchases.map { purchaseList ->
            purchaseList.any { purchase ->
                purchase.products.contains(Constants.BASIC_PRODUCT) && purchase.isAutoRenewing
            }
        }.stateIn(externalScope, SharingStarted.WhileSubscribed(), false)

    // Set to true when a returned purchase is prepaid basic subscription.
    val hasPrepaidBasic: StateFlow<Boolean> =
        billingClientLifecycle.subscriptionPurchases.map { purchaseList ->
            purchaseList.any { purchase ->
                !purchase.isAutoRenewing && purchase.products.contains(Constants.BASIC_PRODUCT)
            }
        }.stateIn(externalScope, SharingStarted.WhileSubscribed(), false)

    // Set to true when a returned purchases is an auto-renewing premium subscription.
    val hasRenewablePremium: StateFlow<Boolean> =
        billingClientLifecycle.subscriptionPurchases.map { purchaseList ->
            purchaseList.any { purchase ->
                purchase.products.contains(Constants.PREMIUM_PRODUCT) && purchase.isAutoRenewing
            }
        }.stateIn(externalScope, SharingStarted.WhileSubscribed(), false)

    // Set to true when a returned purchase is prepaid premium subscription.
    val hasPrepaidPremium: StateFlow<Boolean> =
        billingClientLifecycle.subscriptionPurchases.map { purchaseList ->
            purchaseList.any { purchase ->
                !purchase.isAutoRenewing && purchase.products.contains(Constants.PREMIUM_PRODUCT)
            }
        }.stateIn(externalScope, SharingStarted.WhileSubscribed(), false)

    // Set to true when a returned purchase is a one-time product purchase.
    val hasOneTimeProduct: StateFlow<Boolean> =
        billingClientLifecycle.oneTimeProductPurchases.map { purchaseList ->
            purchaseList.any { purchase ->
                purchase.products.contains(Constants.ONE_TIME_PRODUCT)
            }
        }.stateIn(externalScope, SharingStarted.WhileSubscribed(), false)

    init {
        // Update content from the remote server.
        // We are using a MutableStateFlow so that we can clear the data immediately
        // when the subscription or one-time product purchase changes.
        externalScope.launch {
            remoteDataSource.basicContent.collect {
                _basicContent.emit(it)
            }
        }
        externalScope.launch {
            remoteDataSource.premiumContent.collect {
                _premiumContent.emit(it)
            }
        }

        externalScope.launch {
            remoteDataSource.otpContent.collect {
                _otpContent.emit(it)
            }
        }

        // Коли список покупок змінюється, нам потрібно оновити статус підписки
        // щоб вказати, чи є підписка локальною чи ні. Це локально, якщо
        // API платежів Google Play повертає запис покупки для продукту. Це не
        // локальний, якщо на пристрої немає запису про підписку.
        externalScope.launch {
            billingClientLifecycle.subscriptionPurchases.collect { purchases ->
                Log.i(TAG, "Collected subscription purchases...")
                val currentSubscriptions = subscriptions.value
                val hasChanged = updateLocalPurchaseTokens(
                    subscriptions = currentSubscriptions,
                    purchases = purchases,
                    onetimeProductPurchases = null
                )
                // We only need to update the database if [isLocalPurchase] field needs
                // to be changed.
                if (hasChanged) {
                    localDataSource.updateSubscriptions(currentSubscriptions)
                }
                registerPurchases(purchases)
            }
        }

        // Коли список одноразових покупок товарів змінюється, нам потрібно оновити
        // одноразовий статус продукту, щоб вказати, чи є продукт місцевим чи ні. Це локально, якщо
        // API платежів Google Play повертає запис покупки для продукту. Це не
        // локальний, якщо на пристрої немає запису продукту.
        externalScope.launch {
            billingClientLifecycle.oneTimeProductPurchases.collect { purchases ->
                Log.i(TAG, "Collected one-time product purchases...")
                val currentOneTimeProducts = oneTimeProductPurchases.value
                val hasChanged = updateLocalPurchaseTokens(
                    subscriptions = null,
                    purchases = null,
                    onetimeProductPurchases = currentOneTimeProducts
                )
                // We only need to update the database if [isLocalPurchase] field needs
                // to be changed.
                if (hasChanged) {
                    localDataSource.updateOneTimeProductPurchases(currentOneTimeProducts)
                }
                registerPurchases(purchases)
            }
        }
    }

    suspend fun checkSpecialOfferEligibility(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Вызов к вашему серверу для проверки eligibility
                true
            } catch (e: Exception) {
                Log.e("BillingRepository", "Error checking eligibility", e)
                false
            }
        }
    }

    suspend fun markSpecialOfferAsUsed(userId: String, purchaseToken: String) {
        withContext(Dispatchers.IO) {
            try {
               println("markSpecialOfferAsUsed called for purchaseToken: $purchaseToken")
            } catch (e: Exception) {
                Log.e("BillingRepository", "Error marking offer as used", e)
            }
        }
    }

    /**
     * Update the local database with the subscription status from the remote server.
     * This method is called when the app starts and when the user refreshes the subscription
     * status.
     */
    suspend fun updateSubscriptionsFromNetwork(remoteSubscriptions: List<SubscriptionStatus>?) {
        Log.i(TAG, "Updating subscriptions from remote: ${remoteSubscriptions?.size}")

        val currentSubscriptions = subscriptions.value
        val purchases = billingClientLifecycle.subscriptionPurchases.value

        // Acknowledge the subscription if it is not.
        kotlin.runCatching {
            remoteSubscriptions?.let {
                this.acknowledgeRegisteredSubscriptionPurchaseTokens(it)
            }
        }.onFailure {
            Log.e(
                TAG, "Failed to acknowledge registered subscription purchase tokens: " +
                        "$it"
            )
        }.onSuccess { acknowledgedSubscriptions ->
            Log.i(
                TAG, "Successfully acknowledged registered subscription purchase tokens: " +
                        "$acknowledgedSubscriptions"
            )
            val mergedSubscriptions =
                mergeSubscriptionsAndPurchases(
                    currentSubscriptions,
                    acknowledgedSubscriptions,
                    purchases
                )

            // Store the subscription information when it changes.
            localDataSource.updateSubscriptions(mergedSubscriptions)

            // Update the content when the subscription changes.
            var updateBasic = false
            var updatePremium = false
            acknowledgedSubscriptions?.forEach { subscription ->
                when (subscription.product) {
                    Constants.BASIC_PRODUCT -> {
                        updateBasic = true
                    }

                    Constants.PREMIUM_PRODUCT -> {
                        updatePremium = true
                    }
                }
            }

            if (updateBasic) {
                remoteDataSource.updateBasicContent()
            } else {
                // If we no longer own this content, clear it from the UI.
                _basicContent.emit(null)
            }

            if (updatePremium) {
                remoteDataSource.updatePremiumContent()
            } else {
                // If we no longer own this content, clear it from the UI.
                _premiumContent.emit(null)
            }
        }
    }

    /**
     * Update the local database with the one-time product purchase status from the remote server.
     * This method is called when the app starts and when the user refreshes the one-time product
     * purchase status.
     */
    private suspend fun updateOneTimeProductPurchasesFromNetwork(
        remotePurchases: List<OneTimeProductPurchaseStatus>?
    ) {
        Log.i(TAG, "Updating one-time product purchases from remote: ${remotePurchases?.size}")

        val currentOneTimeProductPurchases = oneTimeProductPurchases.value
        val purchases = billingClientLifecycle.oneTimeProductPurchases.value
        kotlin.runCatching {
            remotePurchases?.let {
                this.acknowledgeRegisteredOneTimeProductPurchases(it)
            }
        }.onFailure {
            Log.e(
                TAG, "Failed to acknowledge registered one time product purchase tokens: " +
                        "$it"
            )
        }.onSuccess { acknowledgedProducts ->
            Log.i(
                TAG,
                "Successfully acknowledged registered one time product purchase tokens. " +
                        "Acknowledged Products: $acknowledgedProducts"
            )
            val mergedOneTimeProducts =
                mergeOneTimeProductPurchasesAndPurchases(
                    currentOneTimeProductPurchases,
                    remotePurchases,
                    purchases
                )

            // Store the one-time product purchase information when it changes.
            localDataSource.updateOneTimeProductPurchases(mergedOneTimeProducts)

            // Update the content when the one-time product ownership changes.
            remotePurchases?.forEach {
                if (it.product == Constants.ONE_TIME_PRODUCT) {
                    if (!it.isConsumed) {
                        // If the user has purchased the one-time product, update the content.
                        remoteDataSource.updateOtpContent()
                    } else {
                        // If user no longer owns this content, clear the content.
                        _otpContent.emit(null)
                    }
                }
            }
        }

    }


    /**
     * Acknowledge subscriptions that have been registered by the server
     * and update local data source.
     * Returns a list of acknowledged subscriptions.
     *
     */
    private suspend fun acknowledgeRegisteredSubscriptionPurchaseTokens(
        remoteSubscriptions: List<SubscriptionStatus>
    ): List<SubscriptionStatus> {
        return remoteSubscriptions.map { sub ->
            if (!sub.isAcknowledged) {
                val acknowledgedSubs = sub.purchaseToken?.let { token ->
                    sub.product?.let { product ->
                        acknowledgeSubscription(product, token)
                    }
                }
                acknowledgedSubs?.let { subList ->
                    localDataSource.updateSubscriptions(subList)
                    subList.map { sub.copy(isAcknowledged = true) }
                } ?: listOf(sub)
            } else {
                Log.d(TAG, "Subscription is already acknowledged")
                listOf(sub)
            }
        }.flatten()
    }

    /**
     * Acknowledge one-time product purchases that have been registered by the server and update
     * local data source.
     * Returns a list of acknowledged one-time product purchases.
     *
     */
    private suspend fun acknowledgeRegisteredOneTimeProductPurchases(
        remoteOneTimeProducts: List<OneTimeProductPurchaseStatus>
    ): List<OneTimeProductPurchaseStatus> {
        val updatedPurchases = mutableListOf<OneTimeProductPurchaseStatus>()
        remoteOneTimeProducts.forEach { purchase ->
            if (!purchase.isAcknowledged) {
                try {
                    val acknowledgedPurchases = purchase.purchaseToken?.let { token ->
                        purchase.product?.let { product ->
                            acknowledgeOneTimeProductPurchase(product, token)
                        }
                    }
                    acknowledgedPurchases?.let { purchases ->
                        localDataSource.updateOneTimeProductPurchases(purchases)
                        updatedPurchases.addAll(purchases)
                    }
                } catch (e: Exception) {
                    throw e
                }
            }
        }
        return updatedPurchases
    }

    /**
     * Объедините предыдущие и новые подписки, просматривая покупки на устройстве.
     *
     * Хотим вернуть список новых подписок, возможно с некоторыми изменениями
     * на основе старых подписок и покупок на устройствах в Google Play Billing.
     * Старые подписки следует сохранять, если они принадлежат другому лицу (subAlreadyOwned).
     * и токен покупки подписки все еще находится на этом устройстве.
     */
    private fun mergeSubscriptionsAndPurchases(
        oldSubscriptions: List<SubscriptionStatus>?,
        newSubscriptions: List<SubscriptionStatus>?,
        purchases: List<Purchase>?
    ): List<SubscriptionStatus> {
        return ArrayList<SubscriptionStatus>().apply {
            if (purchases != null) {
                // Record which purchases are local and can be managed on this device.
                updateLocalPurchaseTokens(
                    subscriptions = newSubscriptions,
                    onetimeProductPurchases = null,
                    purchases = purchases
                )
            }
            if (newSubscriptions != null) {
                addAll(newSubscriptions)
            }
            // Find old subscriptions that are in purchases but not in new subscriptions.
            if (purchases != null && oldSubscriptions != null) {
                for (oldSubscription in oldSubscriptions) {
                    if (oldSubscription.subAlreadyOwned && oldSubscription.isLocalPurchase) {
                        // This old subscription was previously marked as "already owned" by
                        // another user. It should be included in the output if the product
                        // and purchase token match their previous value.
                        for (purchase in purchases) {
                            if (purchase.products[0] == oldSubscription.product &&
                                purchase.purchaseToken == oldSubscription.purchaseToken
                            ) {
                                // The old subscription that was already owned subscription should
                                // be added to the new subscriptions.
                                // Look through the new subscriptions to see if it is there.
                                var foundNewSubscription = false
                                newSubscriptions?.let {
                                    for (newSubscription in it) {
                                        if (newSubscription.product == oldSubscription.product) {
                                            foundNewSubscription = true
                                        }
                                    }
                                }
                                if (!foundNewSubscription) {
                                    // The old subscription should be added to the output.
                                    // It matches a local purchase.
                                    add(oldSubscription)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Merge the previous one-time product purchases and new one-time product purchases
     * by looking at on-device purchases.
     *
     * We want to return the list of new one-time product purchases, possibly with some modifications
     * based on old one-time product purchases and the on-devices purchases from Google Play Billing.
     * Old one-time product purchases should be retained if they are owned by someone else (subAlreadyOwned)
     * and the purchase token for the one-time product purchase is still on this device.
     */
    private fun mergeOneTimeProductPurchasesAndPurchases(
        oldOneTimeProductPurchases: List<OneTimeProductPurchaseStatus>?,
        newOneTimeProductPurchases: List<OneTimeProductPurchaseStatus>?,
        purchases: List<Purchase>?
    ): List<OneTimeProductPurchaseStatus> {
        return ArrayList<OneTimeProductPurchaseStatus>().apply {
            if (purchases != null) {
                // Record which purchases are local and can be managed on this device.
                updateLocalPurchaseTokens(
                    subscriptions = null,
                    purchases = purchases,
                    onetimeProductPurchases = newOneTimeProductPurchases
                )
            }
            if (newOneTimeProductPurchases != null) {
                addAll(newOneTimeProductPurchases)
            }
            // Find old one-time product purchases that are in purchases but not in new one-time product purchases.
            if (purchases != null && oldOneTimeProductPurchases != null) {
                for (oldOneTimeProductPurchase in oldOneTimeProductPurchases) {
                    if (oldOneTimeProductPurchase.isAlreadyOwned && oldOneTimeProductPurchase.isLocalPurchase) {
                        // This old one-time product purchase was previously marked as "already owned" by
                        // another user. It should be included in the output if the product
                        // and purchase token match their previous value.
                        for (purchase in purchases) {
                            if (purchase.products[0] == oldOneTimeProductPurchase.product &&
                                purchase.purchaseToken == oldOneTimeProductPurchase.purchaseToken
                            ) {
                                // The old one-time product purchase that was already owned one-time product purchase should
                                // be added to the new one-time product purchases.
                                // Look through the new one-time product purchases to see if it is there.
                                var foundNewOneTimeProductPurchase = false
                                newOneTimeProductPurchases?.let {
                                    for (newOneTimeProductPurchase in it) {
                                        if (newOneTimeProductPurchase.product == oldOneTimeProductPurchase.product) {
                                            foundNewOneTimeProductPurchase = true
                                        }
                                    }
                                }
                                if (!foundNewOneTimeProductPurchase) {
                                    // The old one-time product purchase should be added to the output.
                                    // It matches a local purchase.
                                    add(oldOneTimeProductPurchase)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Измените поле isLocalPurchase покупки продукта на основе списка местных покупок.
     * Возвращает true, если какое-либо из значений изменилось.
     */
    private fun updateLocalPurchaseTokens(
        subscriptions: List<SubscriptionStatus>?,
        onetimeProductPurchases: List<OneTimeProductPurchaseStatus>?,
        purchases: List<Purchase>?
    ): Boolean {
        var hasChanged = false
        subscriptions?.forEach { subscription ->
            val matchingPurchase = purchases?.find { purchase ->
                purchase.products.any { it == subscription.product }
            }
            val isLocalPurchase = matchingPurchase != null
            if (subscription.isLocalPurchase != isLocalPurchase) {
                subscription.isLocalPurchase = isLocalPurchase
                subscription.purchaseToken =
                    matchingPurchase?.purchaseToken ?: subscription.purchaseToken
                hasChanged = true
            }
        }

        onetimeProductPurchases?.forEach { otp ->
            val matchingPurchase = purchases?.find { purchase ->
                purchase.products.any { it == otp.product }
            }
            val isLocalPurchase = matchingPurchase != null
            if (otp.isLocalPurchase != isLocalPurchase) {
                otp.isLocalPurchase = isLocalPurchase
                hasChanged = true
            }
        }

        return hasChanged
    }

    /**
     * Fetch subscriptions from the server and update local data source.
     */
    suspend fun fetchSubscriptions(): Result<Unit> =
        externalScope.async {
            try {
                val subscriptions = remoteDataSource.fetchSubscriptionStatus()
                localDataSource.updateSubscriptions(subscriptions)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.await()

    /**
     * Fetch one-time product purchases from the server and update local data source.
     */
    suspend fun fetchOneTimeProductPurchases(): Result<Unit> =
        externalScope.async {
            try {
                val oneTimeProductPurchases = remoteDataSource.fetchOneTimeProductPurchaseStatus()
                localDataSource.updateOneTimeProductPurchases(oneTimeProductPurchases)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.await()

    /**
     * Register subscription to this account and update local data source.
     */
    suspend fun registerSubscription(product: String, purchaseToken: String): Result<Unit> {
        return externalScope.async {
            try {
                val subs =
                    remoteDataSource.registerSubscription(
                        product = product,
                        purchaseToken = purchaseToken
                    )
                updateSubscriptionsFromNetwork(subs)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.await()
    }

    /**
     * Register one-time product purchase to this account and update local data source.
     */
    private suspend fun registerOneTimeProductPurchase(
        product: String,
        purchaseToken: String
    ): Result<Unit> {
        return externalScope.async {
            try {
                val oneTimeProductPurchases =
                    remoteDataSource.registerOneTimeProductPurchase(
                        product = product,
                        purchaseToken = purchaseToken
                    )
                updateOneTimeProductPurchasesFromNetwork(oneTimeProductPurchases)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }.await()
    }

    /**
     * Transfer subscription to this account and update local data source.
     */
    suspend fun transferSubscription(product: String, purchaseToken: String) {
        externalScope.launch {
            remoteDataSource.postTransferSubscriptionSync(
                product = product,
                purchaseToken = purchaseToken
            )
        }.join()
    }

    /**
     * Register Instance ID.
     */
    fun registerInstanceId(instanceId: String) {
        externalScope.launch {
            try {
                remoteDataSource.postRegisterInstanceId(instanceId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register the instance ID - ${e.localizedMessage}")
            }
        }
    }

    /**
     * Unregister Instance ID.
     */
    fun unregisterInstanceId(instanceId: String) {
        externalScope.launch {
            try {
                remoteDataSource.postUnregisterInstanceId(instanceId)
            } catch (e: Exception) {
                // In general, this should trigger error event (eg. by using Result object),
                // then ViewModel should collect it and show appropriate error to user.
                Log.e(TAG, "Failed to register the instance ID - ${e.localizedMessage}")
            }
        }
    }

    /**
     * Acknowledge subscription to this account.
     */
    private suspend fun acknowledgeSubscription(product: String, purchaseToken: String)
            : List<SubscriptionStatus>? {
        val result = remoteDataSource.postAcknowledgeSubscription(
            product = product,
            purchaseToken = purchaseToken
        )
        return suspendCoroutine { continuation ->
            continuation.resume(result)
        }
    }

    /**
     * Acknowledge one-time product purchase to this account.
     */
    private suspend fun acknowledgeOneTimeProductPurchase(product: String, purchaseToken: String)
            : List<OneTimeProductPurchaseStatus>? {
        val result = remoteDataSource.postAcknowledgeOneTimeProductPurchase(
            product = product,
            purchaseToken = purchaseToken
        )
        return suspendCoroutine { continuation ->
            continuation.resume(result)
        }
    }

    /**
     * Consume one-time product purchase to this account.
     */
    suspend fun consumeOneTimeProductPurchase(product: String, purchaseToken: String)
            : List<OneTimeProductPurchaseStatus>? {
        val result = remoteDataSource.postConsumeOneTimeProductPurchase(
            product = product,
            purchaseToken = purchaseToken
        )
        return suspendCoroutine { continuation ->
            continuation.resume(result)
        }
    }

    /**
     * Update subscriptions and one-time product purchases from network.
     */
    fun queryProducts() {
        billingClientLifecycle.querySubscriptionPurchases()
        billingClientLifecycle.queryOneTimeProductPurchases()
    }

    /**
     * Register purchases to this account.
     */
    suspend fun registerPurchases(purchaseList: List<Purchase>): Result<Unit> {
        val registerFunctions = mapOf(
            Constants.ONE_TIME_PRODUCT to ::registerOneTimeProductPurchase,
            Constants.BASIC_PRODUCT to ::registerSubscription,
            Constants.PREMIUM_PRODUCT to ::registerSubscription
        )
        return withContext(externalScope.coroutineContext) {
            try {
                purchaseList.forEach { purchase ->
                    purchase.products.forEach { product ->
                        val purchaseToken = purchase.purchaseToken
                        val registerFunction = registerFunctions[product]
                        if (registerFunction != null) {
                            Log.d(TAG, "Registering $product with token $purchaseToken")
                            registerFunction(product, purchaseToken)
                        } else {
                            Log.d(TAG, "Unknown product: $product")
                        }
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Delete local user data when the user signs out.
     */
    suspend fun deleteLocalUserData() {
        withContext(externalScope.coroutineContext) {
            localDataSource.deleteLocalUserSubscriptionsData()
            localDataSource.deleteLocalUserOneTimeProductPurchases()
            _basicContent.emit(null)
            _premiumContent.emit(null)
        }
    }

    companion object {
        private const val TAG = "BillingRepository"

        @Volatile
        private var INSTANCE: BillingRepository? = null

        fun getInstance(
            localDataSource: BillingLocalDataSource,
            billingRemoteDataSource: BillingRemoteDataSource,
            billingClientLifecycle: BillingClientLifecycle,
            externalScope: CoroutineScope =
                CoroutineScope(SupervisorJob() + Dispatchers.Default)
        ): BillingRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingRepository(
                    localDataSource, billingRemoteDataSource, billingClientLifecycle, externalScope
                )
                    .also { INSTANCE = it }
            }
    }
}
