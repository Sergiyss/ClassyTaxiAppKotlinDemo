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

package com.dev.billingwebnauts.data.network.firebase

import com.dev.billingwebnauts.data.ContentResource
import com.dev.billingwebnauts.data.network.retrofit.ServerFunctionsImpl
import com.dev.billingwebnauts.data.otps.OneTimeProductPurchaseStatus
import com.dev.billingwebnauts.data.subscriptions.SubscriptionStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface to perform the remote API calls.
 *
 * Server updates for loading, basicContent, premiumContent will be communicated
 * through [StateFlow] variables.
 */
interface ServerFunctions {

    /**
     * True when there are pending network requests.
     */
    val loading: StateFlow<Boolean>

    /**
     * The basic content URL.
     */
    val basicContent: StateFlow<ContentResource?>

    /**
     * The premium content URL.
     */
    val premiumContent: StateFlow<ContentResource?>

    /**
     * The one time product content URL.
     */
    val otpContent: StateFlow<ContentResource?>

    /**
     * Fetch basic content and post results to [basicContent].
     * This will fail if the user does not have a basic subscription.
     */
    suspend fun updateBasicContent()

    /**
     * Fetch premium content and post results to [premiumContent].
     * This will fail if the user does not have a premium subscription.
     */
    suspend fun updatePremiumContent()

    /**
     * Fetch a one-time product content and post results to [otpContent].
     * This will fail if the user does not have a one-time product purchase.
     */
    suspend fun updateOtpContent()

    /**
     * Fetches subscription data from the server.
     */
    suspend fun fetchSubscriptionStatus(): List<SubscriptionStatus>

    /**
     * Fetches one-time product purchases data from the server.
     */
    suspend fun fetchOtpStatus(): List<OneTimeProductPurchaseStatus>

    /**
     * Register a subscription with the server and return results.
     */
    suspend fun registerSubscription(
        product: String,
        purchaseToken: String
    ): List<SubscriptionStatus>

    /**
     * Register a one-time product with the server and return results.
     */
    suspend fun registerOtp(
        product: String,
        purchaseToken: String
    ): List<OneTimeProductPurchaseStatus>

    /**
     * Transfer subscription to this account posts.
     */
    suspend fun transferSubscription(
        product: String,
        purchaseToken: String
    ): List<SubscriptionStatus>

    /**
     * Register Instance ID when the user signs in or the token is refreshed.
     */
    suspend fun registerInstanceId(instanceId: String)

    /**
     * Unregister when the user signs out.
     */
    suspend fun unregisterInstanceId(instanceId: String)

    /**
     * Send a subscription purchase object to server for acknowledgement.
     */
    suspend fun acknowledgeSubscription(
        product: String,
        purchaseToken: String
    ): List<SubscriptionStatus>

    /**
     * Send a one-time product purchase object to server for acknowledgement.
     */
    suspend fun acknowledgeOtp(
        product: String,
        purchaseToken: String
    ): List<OneTimeProductPurchaseStatus>

    /**
     * Send a one-time product purchase object to server for consumption.
     */
    suspend fun consumeOtp(
        product: String,
        purchaseToken: String
    ): List<OneTimeProductPurchaseStatus>

    companion object {
        @Volatile
        private var INSTANCE: ServerFunctions? = null

        fun getInstance(): ServerFunctions =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServerFunctionsImpl().also { INSTANCE = it }
            }
    }
}
