package com.dev.billingwebnauts.utils


import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dev.billingwebnauts.data.subscriptions.SubscriptionStatus
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


private const val BASIC_ON_HOLD = "Basic plan (on hold)"
private const val BASIC_PAUSED = "Basic plan (paused)"
private const val BASIC_ON_GRACE_PERIOD = "Basic plan (payment issue)"
private const val BASIC_SUB_RESTORE = "Basic plan (canceled)"
private const val BASIC_REGULAR_PURCHASE = "Basic plan (current)"
private const val BASIC_NOT_PURCHASED = "Basic plan"

private const val PREMIUM_ON_HOLD = "Premium plan (on hold)"
private const val PREMIUM_PAUSED = "Premium plan (paused)"
private const val PREMIUM_ON_GRACE_PERIOD = "Premium plan (payment issue)"
private const val PREMIUM_SUB_RESTORE = "Premium plan (canceled)"
private const val PREMIUM_REGULAR_PURCHASE = "Premium plan (current)"
private const val PREMIUM_NOT_PURCHASED = "Premium plan"

@RunWith(AndroidJUnit4::class)
class SubscriptionUtilitiesTest {

    private lateinit var context: Application

    @Before
    fun setupContext() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun basicTextForSubscription_returnsBasicTextForOnHoldSub() {

        val onHoldSubscription = SubscriptionStatus(
            subAlreadyOwned = false,
            isEntitlementActive = false,
            isAccountHold = true,
            isLocalPurchase = true
        )

        val result = basicTextForSubscription(context.resources, onHoldSubscription)
        assertThat(result, `is`(BASIC_ON_HOLD))
    }

    @Test
    fun basicTextForSubscription_returnsBasicTextForOnHoldAndNotOnDeviceSub() {


        val onHoldAndNonLocalSubscription = SubscriptionStatus(
            subAlreadyOwned = false,
            isEntitlementActive = false,
            isAccountHold = true,
            isLocalPurchase = false
        )

        val result = basicTextForSubscription(context.resources, onHoldAndNonLocalSubscription)
        assertThat(result, `is`("$BASIC_ON_HOLD*"))
    }

    @Test
    fun basicTextForSubscription_returnsBasicTextForPausedSub() {

        val pausedSubscription = SubscriptionStatus(
            subAlreadyOwned = false,
            isEntitlementActive = false,
            isPaused = true,
            isLocalPurchase = true
        )

        val result = basicTextForSubscription(context.resources, pausedSubscription)
        assertThat(result, `is`(BASIC_PAUSED))
    }

    @Test
    fun basicTextForSubscription_returnsBasicTextForGracePeriodSub() {

        val onGraceSubscription = SubscriptionStatus(
            subAlreadyOwned = false,
            isEntitlementActive = true,
            isGracePeriod = true,
            isLocalPurchase = true
        )

        val result = basicTextForSubscription(context.resources, onGraceSubscription)
        assertThat(result, `is`(BASIC_ON_GRACE_PERIOD))
    }

    @Test
    fun basicTextForSubscription_returnsBasicTextForSubRestore() {

        val willNotRenewSubscription = SubscriptionStatus(
            subAlreadyOwned = false,
            isEntitlementActive = true,
            willRenew = false,
            isLocalPurchase = true
        )

        val result = basicTextForSubscription(context.resources, willNotRenewSubscription)
        assertThat(result, `is`(BASIC_SUB_RESTORE))
    }

    @Test
    fun basicTextForSubscription_returnsBasicTextForRegularSub() {

        val basicProductSubscription = SubscriptionStatus(
            subAlreadyOwned = false,
            isEntitlementActive = true,
            product = "basic_subscription",
            isLocalPurchase = true,
            willRenew = true
        )

        val result = basicTextForSubscription(context.resources, basicProductSubscription)
        assertThat(result, `is`(BASIC_REGULAR_PURCHASE))
    }

    @Test
    fun basicTextForSubscription_returnsBasicTextForNotPurchasedSub() {

        val nonBasicProductSubscription = SubscriptionStatus(
            subAlreadyOwned = false,
            isEntitlementActive = true,
            product = "premium_subscription",
            isLocalPurchase = true,
            willRenew = true
        )

        val result = basicTextForSubscription(context.resources, nonBasicProductSubscription)
        assertThat(result, `is`(BASIC_NOT_PURCHASED))
    }

    @Test
    fun premiumTextForSubscription_returnsPremiumTextForOnHoldSub() {


        val onHoldSubscription = SubscriptionStatus(
            subAlreadyOwned = false,
            isEntitlementActive = false,
            isAccountHold = true,
            isLocalPurchase = true
        )

        val result = premiumTextForSubscription(context.resources, onHoldSubscription)
        assertThat(result, `is`(PREMIUM_ON_HOLD))
    }

    @Test
    fun premiumTextForSubscription_returnsPremiumTextForOnHoldAndNotOnDeviceSub() {


        val onHoldAndNonLocalSubscription = SubscriptionStatus(
            subAlreadyOwned = false,
            isEntitlementActive = false,
            isAccountHold = true,
            isLocalPurchase = false
        )

        val result = premiumTextForSubscription(context.resources, onHoldAndNonLocalSubscription)
        assertThat(result, `is`("$PREMIUM_ON_HOLD*"))
    }

    @Test
    fun premiumTextForSubscription_returnsBasicTextForPausedSub() {

        val pausedSubscription = SubscriptionStatus(
            subAlreadyOwned = false,
            isEntitlementActive = false,
            isPaused = true,
            isLocalPurchase = true
        )

        val result = premiumTextForSubscription(context.resources, pausedSubscription)
        assertThat(result, `is`(PREMIUM_PAUSED))
    }

    @Test
    fun premiumTextForSubscription_returnsBasicTextForGracePeriodSub() {

        val onGraceSubscription = SubscriptionStatus(
            subAlreadyOwned = false,
            isEntitlementActive = true,
            isGracePeriod = true,
            isLocalPurchase = true
        )

        val result = premiumTextForSubscription(context.resources, onGraceSubscription)
        assertThat(result, `is`(PREMIUM_ON_GRACE_PERIOD))
    }

    @Test
    fun premiumTextForSubscription_returnsBasicTextForSubRestore() {

        val willNotRenewSubscription = SubscriptionStatus(
            subAlreadyOwned = false,
            isEntitlementActive = true,
            willRenew = false,
            isLocalPurchase = true
        )

        val result = premiumTextForSubscription(context.resources, willNotRenewSubscription)
        assertThat(result, `is`(PREMIUM_SUB_RESTORE))
    }

    @Test
    fun premiumTextForSubscription_returnsPremiumTextForRegularSub() {

        val premiumProductSubscription = SubscriptionStatus(
            subAlreadyOwned = false,
            isEntitlementActive = true,
            product = "premium_subscription",
            isLocalPurchase = true,
            willRenew = true
        )

        val result = premiumTextForSubscription(context.resources, premiumProductSubscription)
        assertThat(result, `is`(PREMIUM_REGULAR_PURCHASE))
    }

    @Test
    fun premiumTextForSubscription_returnsPremiumTextForNotPurchasedSub() {

        val nonPremiumProductSubscription = SubscriptionStatus(
            subAlreadyOwned = false,
            isEntitlementActive = true,
            product = "basic_subscription",
            isLocalPurchase = true,
            willRenew = true
        )

        val result = premiumTextForSubscription(context.resources, nonPremiumProductSubscription)
        assertThat(result, `is`(PREMIUM_NOT_PURCHASED))
    }
}