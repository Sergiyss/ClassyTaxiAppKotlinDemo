/*
 * Copyright 2023 Google LLC. All rights reserved.
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

import com.dev.billingwebnauts.data.ContentResource
import com.dev.billingwebnauts.data.otps.OneTimeProductPurchaseStatus
import com.dev.billingwebnauts.data.otps.OneTimeProductPurchaseStatusList
import com.dev.billingwebnauts.data.subscriptions.SubscriptionStatus
import com.dev.billingwebnauts.data.subscriptions.SubscriptionStatusList
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/**
 * [BillingApiCall] defines the API endpoints that are called in [ServerFunctionsImpl].
 */
interface BillingApiCall {

    // Fetch Basic content.
    @GET("content_basic")
    suspend fun fetchBasicContent(): ContentResource

    // Fetch Premium content.
    @GET("content_premium")
    suspend fun fetchPremiumContent(): ContentResource

    // Fetch Subscription Status.
    @GET("subscription_status")
    suspend fun fetchSubscriptionStatus(): Response<SubscriptionStatusList>

    // Registers Instance ID for Firebase Cloud Messaging.
    @PUT("instanceId_register")
    suspend fun registerInstanceID(@Body instanceId: Map<String, String>): String

    // Unregisters Instance ID for Firebase Cloud Messaging.
    @PUT("instanceId_unregister")
    suspend fun unregisterInstanceID(@Body instanceId: Map<String, String>): String

    // Registers subscription status to the server and get updated list of subscriptions
    @PUT("subscription_register")
    suspend fun registerSubscription(@Body registerStatus: SubscriptionStatus):
            Response<SubscriptionStatusList>

    // Transfers subscription status to another account.
    @PUT("subscription_transfer")
    suspend fun transferSubscription(@Body transferStatus: SubscriptionStatus):
            Response<SubscriptionStatusList>

    // Acknowledges subscription status to the server and get updated list of subscriptions
    @PUT("acknowledge_purchase")
    suspend fun acknowledgeSubscription(@Body acknowledge: SubscriptionStatus):
            Response<SubscriptionStatusList>

    // Fetch One Time Product content.
    @GET("content_otp")
    suspend fun fetchOtpContent(): ContentResource

    // Fetch One Time Product Status.
    @GET("otp_status")
    suspend fun fetchOtpStatus(): Response<OneTimeProductPurchaseStatusList>

    // Registers One Time Product status to the server and get updated list of One-Time Product
    // purchases.
    @PUT("otp_register")
    suspend fun registerOtp(@Body registerStatus: OneTimeProductPurchaseStatus):
            Response<OneTimeProductPurchaseStatusList>

    // Acknowledges One Time Product status to the server and get updated list of One-Time Product
    // purchases.
    @PUT("otp_acknowledge")
    suspend fun acknowledgeOtp(@Body acknowledge: OneTimeProductPurchaseStatus):
            Response<OneTimeProductPurchaseStatusList>

    // Consumes One Time Product status to the server and get updated list of One-Time Product
    // purchases.
    @PUT("otp_consume")
    suspend fun consumeOtp(@Body consume: OneTimeProductPurchaseStatus):
            Response<OneTimeProductPurchaseStatusList>
}
