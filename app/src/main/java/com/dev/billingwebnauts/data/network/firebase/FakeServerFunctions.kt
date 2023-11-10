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

import com.dev.billingwebnauts.Constants
import com.dev.billingwebnauts.gpbl.isBasicContent
import com.dev.billingwebnauts.gpbl.isPremiumContent
import com.dev.billingwebnauts.data.ContentResource
import com.dev.billingwebnauts.data.otps.OneTimeProductPurchaseStatus
import com.dev.billingwebnauts.data.subscriptions.SubscriptionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of [ServerFunctions].
 */
class FakeServerFunctions: ServerFunctions {
    private var subscriptions: List<SubscriptionStatus> = emptyList()
    private val _basicContent = MutableStateFlow<ContentResource?>(null)
    private val _premiumContent = MutableStateFlow<ContentResource?>(null)
    private val _otpContent = MutableStateFlow<ContentResource?>(null)

    override val basicContent = _basicContent.asStateFlow()
    override val premiumContent = _premiumContent.asStateFlow()
    override val otpContent = _otpContent.asStateFlow()
    override val loading: StateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Fetch fake basic content and post results to [basicContent].
     * This will fail if the user does not have a basic subscription.
     */
    override suspend fun updateBasicContent() {
        if (subscriptions.isEmpty()) {
            _basicContent.emit(null)
            return
        }
        // Premium subscriptions also give access to basic content.
        if (subscriptions[0].isBasicContent || isPremiumContent(subscriptions[0])) {
            _basicContent.emit(ContentResource("https://example.com/basic.jpg"))
        } else {
            _basicContent.emit(null)
        }
    }

    /**
     * Fetch fake premium content and post results to [premiumContent].
     * This will fail if the user does not have a premium subscription.
     */
    override suspend fun updatePremiumContent() {
        if (subscriptions.isEmpty()) {
            _premiumContent.emit(null)
            return
        }
        if (isPremiumContent(subscriptions[0])) {
            _premiumContent.emit(ContentResource("https://example.com/premium.jpg"))
        } else {
            _premiumContent.emit(null)
        }
    }

    override suspend fun updateOtpContent() {
        TODO("Not yet implemented")
    }

    /**
     * Fetches fake subscription data and posts successful results to [subscriptions].
     */
    override suspend fun fetchSubscriptionStatus(): List<SubscriptionStatus> {
        subscriptions = ArrayList<SubscriptionStatus>().apply {
            nextFakeSubscription()?.let {
                add(it)
            }
        }
        return subscriptions
    }

    override suspend fun fetchOtpStatus(): List<OneTimeProductPurchaseStatus>{
        return listOf(OneTimeProductPurchaseStatus())
    }

    override suspend fun registerSubscription(product: String, purchaseToken: String):
        List<SubscriptionStatus> {
        val result = when (product) {
            Constants.BASIC_PRODUCT -> listOf(createFakeBasicSubscription())
            Constants.PREMIUM_PRODUCT -> listOf(createFakePremiumSubscription())
            else -> listOf(
                createAlreadyOwnedSubscription(
                    product = product, purchaseToken = purchaseToken
                )
            )
        }
        subscriptions = result
        return result
    }

    override suspend fun registerOtp(
        product: String,
        purchaseToken: String
    ): List<OneTimeProductPurchaseStatus> {
        TODO("Not yet implemented")
    }

    override suspend fun transferSubscription(product: String, purchaseToken: String):
        List<SubscriptionStatus> {
        val subscription = createFakeBasicSubscription().apply {
            this.product = product
            this.purchaseToken = purchaseToken
            subAlreadyOwned = false
            isEntitlementActive = true
        }
        subscriptions = listOf(subscription)
        return subscriptions
    }

    override suspend fun registerInstanceId(instanceId: String) = Unit
    override suspend fun unregisterInstanceId(instanceId: String) = Unit
    override suspend fun acknowledgeSubscription(
        product: String,
        purchaseToken: String
    ): List<SubscriptionStatus> {
        TODO("Not yet implemented")
    }

    override suspend fun acknowledgeOtp(
        product: String,
        purchaseToken: String
    ): List<OneTimeProductPurchaseStatus> {
        TODO("Not yet implemented")
    }

    override suspend fun consumeOtp(
        product: String,
        purchaseToken: String
    ): List<OneTimeProductPurchaseStatus> {
        TODO("Not yet implemented")
    }

    /**
     * Create a local record of a subscription that is already owned by someone else.
     * Created when the server returns HTTP 409 CONFLICT after a subscription registration request.
     */
    private fun createAlreadyOwnedSubscription(
        product: String,
        purchaseToken: String
    ): SubscriptionStatus {
        return SubscriptionStatus().apply {
            this.product = product
            this.purchaseToken = purchaseToken
            isEntitlementActive = false
            subAlreadyOwned = true
        }
    }

    private var fakeDataIndex = 0

    private fun nextFakeSubscription(): SubscriptionStatus? {
        val subscription = when (fakeDataIndex) {
            0 -> null
            1 -> createFakeBasicSubscription()
            2 -> createFakePremiumSubscription()
            3 -> createFakeAccountPausedSubscription()
            4 -> createFakeAccountHoldSubscription()
            5 -> createFakeGracePeriodSubscription()
            6 -> createFakeAlreadyOwnedSubscription()
            7 -> createFakeCanceledBasicSubscription()
            8 -> createFakeCanceledPremiumSubscription()
            else -> null // Unknown fake index, just pick one.
        }
        // Iterate through fake data for testing purposes.
        fakeDataIndex = (fakeDataIndex + 1) % 9
        return subscription
    }

    private fun createFakeBasicSubscription(): SubscriptionStatus {
        return SubscriptionStatus().apply {
            isEntitlementActive = true
            willRenew = true
            product = Constants.BASIC_PRODUCT
            isAccountHold = false
            isGracePeriod = false
            purchaseToken = null
            subAlreadyOwned = false
        }
    }

    private fun createFakePremiumSubscription(): SubscriptionStatus {
        return SubscriptionStatus().apply {
            isEntitlementActive = true
            willRenew = true
            product = Constants.PREMIUM_PRODUCT
            isAccountHold = false
            isGracePeriod = false
            purchaseToken = null
            subAlreadyOwned = false
        }
    }

    private fun createFakeAccountHoldSubscription(): SubscriptionStatus {
        return SubscriptionStatus().apply {
            isEntitlementActive = false
            willRenew = true
            product = Constants.PREMIUM_PRODUCT
            isAccountHold = true
            isGracePeriod = false
            purchaseToken = null
            subAlreadyOwned = false
        }
    }

    private fun createFakeAccountPausedSubscription(): SubscriptionStatus {
        return SubscriptionStatus().apply {
            isEntitlementActive = false
            willRenew = true
            product = Constants.PREMIUM_PRODUCT
            isPaused = true
            isGracePeriod = false
            purchaseToken = null
            subAlreadyOwned = false
        }
    }

    private fun createFakeGracePeriodSubscription(): SubscriptionStatus {
        return SubscriptionStatus().apply {
            isEntitlementActive = true
            willRenew = true
            product = Constants.BASIC_PRODUCT
            isAccountHold = false
            isGracePeriod = true
            purchaseToken = null
            subAlreadyOwned = false
        }
    }

    private fun createFakeAlreadyOwnedSubscription(): SubscriptionStatus {
        return SubscriptionStatus().apply {
            isEntitlementActive = false
            willRenew = true
            product = Constants.BASIC_PRODUCT
            isAccountHold = false
            isGracePeriod = false
            purchaseToken = Constants.BASIC_PRODUCT // Very fake data.
            subAlreadyOwned = true
        }
    }

    private fun createFakeCanceledBasicSubscription(): SubscriptionStatus {
        return SubscriptionStatus().apply {
            isEntitlementActive = true
            willRenew = false
            product = Constants.BASIC_PRODUCT
            isAccountHold = false
            isGracePeriod = false
            purchaseToken = null
            subAlreadyOwned = false
        }
    }

    private fun createFakeCanceledPremiumSubscription(): SubscriptionStatus {
        return SubscriptionStatus().apply {
            isEntitlementActive = true
            willRenew = false
            product = Constants.PREMIUM_PRODUCT
            isAccountHold = false
            isGracePeriod = false
            purchaseToken = null
            subAlreadyOwned = false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FakeServerFunctions? = null

        fun getInstance(): ServerFunctions =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FakeServerFunctions().also { INSTANCE = it }
            }
    }
}