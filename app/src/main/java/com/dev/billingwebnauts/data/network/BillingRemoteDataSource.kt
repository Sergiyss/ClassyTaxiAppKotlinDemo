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

package com.dev.billingwebnauts.data.network

import com.dev.billingwebnauts.data.network.firebase.ServerFunctions
import com.dev.billingwebnauts.data.otps.OneTimeProductPurchaseStatus
import com.dev.billingwebnauts.data.subscriptions.SubscriptionStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Execute network requests on the network thread.
 * Fetch data from a remote server object.
 */
class BillingRemoteDataSource private constructor(
    private val serverFunctions: ServerFunctions
) {
    /**
     * True when there are pending network requests.
     */
    val loading: StateFlow<Boolean>
        get() = serverFunctions.loading

    /**
     * State Flow with the basic subscription content.
     */
    val basicContent = serverFunctions.basicContent

    /**
     * State Flow with the premium subscription content.
     */
    val premiumContent = serverFunctions.premiumContent

    /**
     * State Flow with the one-time product content.
     */
    val otpContent = serverFunctions.otpContent

    /**
     * GET basic content.
     */
    suspend fun updateBasicContent() = serverFunctions.updateBasicContent()

    /**
     * GET premium content.
     */
    suspend fun updatePremiumContent() = serverFunctions.updatePremiumContent()

    /**
     * GET one-time product content.
     */
    suspend fun updateOtpContent() = serverFunctions.updateOtpContent()

    /**
     * GET request for subscription status.
     */
    suspend fun fetchSubscriptionStatus() = serverFunctions.fetchSubscriptionStatus()

    /**
     * GET request for one-time product status.
     */
    suspend fun fetchOneTimeProductPurchaseStatus() = serverFunctions.fetchOtpStatus()

    /**
     * POST request to register subscription.
     */
    suspend fun registerSubscription(
        product: String,
        purchaseToken: String
    ): List<SubscriptionStatus> {
        return serverFunctions.registerSubscription(
            product = product,
            purchaseToken = purchaseToken
        )
    }

    /**
     * POST request to register one-time product.
     */
    suspend fun registerOneTimeProductPurchase(
        product: String,
        purchaseToken: String
    ): List<OneTimeProductPurchaseStatus> {
        return serverFunctions.registerOtp(
            product = product,
            purchaseToken = purchaseToken
        )
    }

    /**
     * POST request to transfer a subscription that is owned by someone else.
     */
    suspend fun postTransferSubscriptionSync(product: String, purchaseToken: String) {
        serverFunctions.transferSubscription(product = product, purchaseToken = purchaseToken)
    }

    /**
     * POST request to register an Instance ID.
     */
    suspend fun postRegisterInstanceId(instanceId: String) {
        serverFunctions.registerInstanceId(instanceId)
    }

    /**
     * POST request to unregister an Instance ID.
     */
    suspend fun postUnregisterInstanceId(instanceId: String) {
        serverFunctions.unregisterInstanceId(instanceId)
    }

    /**
     * POST request to acknowledge a subscription.
     */
    suspend fun postAcknowledgeSubscription(
        product: String,
        purchaseToken: String
    ): List<SubscriptionStatus> {
        return serverFunctions.acknowledgeSubscription(
            product = product,
            purchaseToken = purchaseToken
        )
    }

    /**
     * POST request to acknowledge a one-time product.
     */
    suspend fun postAcknowledgeOneTimeProductPurchase(
        product: String,
        purchaseToken: String
    ): List<OneTimeProductPurchaseStatus> {
        return serverFunctions.acknowledgeOtp(
            product = product,
            purchaseToken = purchaseToken
        )
    }

    /**
     * POST request to consume a one-time product.
     */
    suspend fun postConsumeOneTimeProductPurchase(
        product: String,
        purchaseToken: String
    ): List<OneTimeProductPurchaseStatus> {
        return serverFunctions.consumeOtp(
            product = product,
            purchaseToken = purchaseToken
        )
    }


    companion object {
        @Volatile
        private var INSTANCE: BillingRemoteDataSource? = null

        fun getInstance(
            callableFunctions: ServerFunctions
        ): BillingRemoteDataSource =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingRemoteDataSource(callableFunctions).also { INSTANCE = it }
            }
    }
}
