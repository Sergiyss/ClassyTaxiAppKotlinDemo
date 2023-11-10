/*
 * Copyright 2021 Google LLC. All rights reserved.
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

package com.dev.billingwebnauts.data.network.retrofit

import android.util.Log
import com.dev.billingwebnauts.BuildConfig.SERVER_URL
import com.dev.billingwebnauts.data.ContentResource
import com.dev.billingwebnauts.data.network.firebase.ServerFunctions
import com.dev.billingwebnauts.data.network.retrofit.authentication.RetrofitClient
import com.dev.billingwebnauts.data.otps.OneTimeProductPurchaseStatus
import com.dev.billingwebnauts.data.subscriptions.SubscriptionStatus
import java.net.HttpURLConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response

fun <T> Response<T>.errorLog(): String {
    return "Failed to call API (Error code: ${code()}) - ${errorBody()?.string()}"
}

/**
 * Implementation of [ServerFunctions] using Retrofit.
 */
class ServerFunctionsImpl : ServerFunctions {

    private val retrofitClient = RetrofitClient(SERVER_URL, BillingApiCall::class.java)

    /**
     * Track the number of pending server requests.
     */
    private val pendingRequestCounter = PendingRequestCounter()

    private val _subscriptions = MutableStateFlow(emptyList<SubscriptionStatus>())
    private val _basicContent = MutableStateFlow<ContentResource?>(null)
    private val _premiumContent = MutableStateFlow<ContentResource?>(null)
    private val _otpContent = MutableStateFlow<ContentResource?>(null)

    override val loading: StateFlow<Boolean> = pendingRequestCounter.loading
    override val basicContent = _basicContent.asStateFlow()
    override val premiumContent = _premiumContent.asStateFlow()
    override val otpContent = _otpContent.asStateFlow()

    override suspend fun updateBasicContent() {
        pendingRequestCounter.use {
            val response = retrofitClient.getService().fetchBasicContent()
            _basicContent.emit(response)
        }
    }

    override suspend fun updatePremiumContent() {
        pendingRequestCounter.use {
            val response = retrofitClient.getService().fetchPremiumContent()
            _premiumContent.emit(response)
        }
    }

    override suspend fun updateOtpContent() {
        val response = retrofitClient.getService().fetchOtpContent()
        _otpContent.emit(response)
    }

    override suspend fun fetchSubscriptionStatus(): List<SubscriptionStatus> {
        pendingRequestCounter.use {
            val response = retrofitClient.getService().fetchSubscriptionStatus()
            if (!response.isSuccessful) {
                Log.e(TAG, response.errorLog())
                throw Exception("Failed to fetch subscriptions from the server")
            }
            return response.body()?.subscriptions.orEmpty()
        }
    }

    override suspend fun fetchOtpStatus(): List<OneTimeProductPurchaseStatus> {
        pendingRequestCounter.use {
            val response = retrofitClient.getService().fetchOtpStatus()
            if (!response.isSuccessful) {
                Log.e(TAG, response.errorLog())
                throw Exception("Failed to fetch one-time product purchases from the server")
            }
            return response.body()?.oneTimeProductPurchases.orEmpty()
        }
    }

    override suspend fun registerSubscription(
        product: String,
        purchaseToken: String
    ): List<SubscriptionStatus> {
        val data = SubscriptionStatus(
            product = product,
            purchaseToken = purchaseToken
        )
        pendingRequestCounter.use {
            val response = retrofitClient.getService().registerSubscription(data)
            if (response.isSuccessful && response.body() != null) {
                return response.body()?.subscriptions.orEmpty()
            } else {
                if (response.code() == HttpURLConnection.HTTP_CONFLICT) {
                    Log.w(TAG, "Subscription already exists")
                    val oldSubscriptions = _subscriptions.value
                    val newSubscription = SubscriptionStatus.alreadyOwnedSubscription(
                        product, purchaseToken
                    )
                    val newSubscriptions = insertOrUpdateSubscription(
                        oldSubscriptions, newSubscription
                    )
                    _subscriptions.emit(newSubscriptions)
                    return newSubscriptions
                } else {
                    Log.e(TAG, response.errorLog())
                    throw Exception("Failed to register subscription")
                }
            }
        }
    }

    override suspend fun registerOtp(
        product: String,
        purchaseToken: String
    ): List<OneTimeProductPurchaseStatus> {
        val data = OneTimeProductPurchaseStatus(
            product = product,
            purchaseToken = purchaseToken
        )
        pendingRequestCounter.use {
            val response = retrofitClient.getService().registerOtp(data)
            if (response.isSuccessful && response.body() != null) {
                return response.body()?.oneTimeProductPurchases.orEmpty()
            } else {
                if (response.code() == HttpURLConnection.HTTP_CONFLICT) {
                    return emptyList()
                } else {
                    Log.e(TAG, response.errorLog())
                    throw Exception("Failed to register one-time product purchase")
                }
            }
        }
    }

    override suspend fun transferSubscription(product: String, purchaseToken: String):
            List<SubscriptionStatus> {
        val data = SubscriptionStatus().also {
            it.product = product
            it.purchaseToken = purchaseToken
        }
        pendingRequestCounter.use {
            val response = retrofitClient.getService().transferSubscription(data)
            return response.body()?.subscriptions.orEmpty()
        }
    }

    override suspend fun registerInstanceId(instanceId: String) {
        val data = mapOf("instanceId" to instanceId)
        pendingRequestCounter.use {
            retrofitClient.getService().registerInstanceID(data)
        }
    }

    override suspend fun unregisterInstanceId(instanceId: String) {
        val data = mapOf("instanceId" to instanceId)
        pendingRequestCounter.use {
            retrofitClient.getService().unregisterInstanceID(data)
        }
    }

    override suspend fun acknowledgeSubscription(
        product: String,
        purchaseToken: String
    ): List<SubscriptionStatus> {
        val data = SubscriptionStatus(
            product = product,
            purchaseToken = purchaseToken
        )
        pendingRequestCounter.use {
            val response = retrofitClient.getService().acknowledgeSubscription(data)

            if (!response.isSuccessful) {
                Log.e(TAG, response.errorLog())
                throw Exception("Failed to fetch subscriptions from the server")
            }
            return response.body()?.subscriptions.orEmpty()
        }
    }

    override suspend fun acknowledgeOtp(
        product: String,
        purchaseToken: String
    ): List<OneTimeProductPurchaseStatus> {
        val data = OneTimeProductPurchaseStatus(
            product = product,
            purchaseToken = purchaseToken
        )
        pendingRequestCounter.use {
            val response = retrofitClient.getService().acknowledgeOtp(data)

            if (!response.isSuccessful) {
                Log.e(TAG, response.errorLog())
                throw Exception("Failed to fetch one-time products purchases from the server")
            }
            return response.body()?.oneTimeProductPurchases.orEmpty()
        }
    }

    override suspend fun consumeOtp(
        product: String,
        purchaseToken: String
    ): List<OneTimeProductPurchaseStatus> {
        val data = OneTimeProductPurchaseStatus(
            product = product,
            purchaseToken = purchaseToken
        )
        pendingRequestCounter.use {
            val response = retrofitClient.getService().consumeOtp(data)

            if (!response.isSuccessful) {
                Log.e(TAG, response.errorLog())
                throw Exception("Failed to fetch one-time products from the server")
            }
            return response.body()?.oneTimeProductPurchases.orEmpty()
        }
    }

    /**
     * Inserts or updates the subscription to the list of existing com.example.subscriptions.
     *
     * If none of the existing com.example.subscriptions have a product that matches, insert this
     * Product. If a subscription exists with the matching Product, the output list will contain the
     * new subscription instead of the old subscription.
     *
     */
    private fun insertOrUpdateSubscription(
        oldSubscriptions: List<SubscriptionStatus>?,
        newSubscription: SubscriptionStatus
    ): List<SubscriptionStatus> {
        if (oldSubscriptions.isNullOrEmpty()) return listOf(newSubscription)
        return oldSubscriptions.filter { it.product != newSubscription.product } + newSubscription
    }

    companion object {
        private const val TAG = "RemoteServerFunction"
    }
}