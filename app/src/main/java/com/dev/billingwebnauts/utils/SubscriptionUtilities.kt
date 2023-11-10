/*
 * Copyright 2019 Google LLC. All rights reserved.
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

package com.dev.billingwebnauts.utils

import android.content.res.Resources
import com.dev.billingwebnauts.R
import com.dev.billingwebnauts.gpbl.isAccountHold
import com.dev.billingwebnauts.gpbl.isBasicContent
import com.dev.billingwebnauts.gpbl.isGracePeriod
import com.dev.billingwebnauts.gpbl.isPaused
import com.dev.billingwebnauts.gpbl.isPremiumContent
import com.dev.billingwebnauts.gpbl.isSubscriptionRestore
import com.dev.billingwebnauts.data.subscriptions.SubscriptionStatus

/**
 * Return the resource string for the basic subscription button.
 *
 * Add an asterisk if the subscription is not local and might not be modifiable on this device.
 */
fun basicTextForSubscription(res: Resources, subscription: SubscriptionStatus): String {
    val text = when {
        isAccountHold(subscription) -> {
            res.getString(R.string.subscription_option_basic_message_account_hold)
        }
        isPaused(subscription) -> {
            res.getString(R.string.subscription_option_basic_message_account_paused)
        }
        isGracePeriod(subscription) -> {
            res.getString(R.string.subscription_option_basic_message_grace_period)
        }
        isSubscriptionRestore(subscription) -> {
            res.getString(R.string.subscription_option_basic_message_restore)
        }
        subscription.isBasicContent -> {
            res.getString(R.string.subscription_option_basic_message_current)
        }
        else -> {
            res.getString(R.string.subscription_option_basic_message)
        }
    }
    return if (subscription.isLocalPurchase) {
        text
    } else {
        // No local record, so the subscription cannot be managed on this device.
        "$text*"
    }
}

/**
 * Return the resource string for the premium subscription button.
 *
 * Add an asterisk if the subscription is not local and might not be modifiable on this device.
 */
fun premiumTextForSubscription(res: Resources, subscription: SubscriptionStatus): String {
    val text = when {
        isAccountHold(subscription) -> {
            res.getString(R.string.subscription_option_premium_message_account_hold)
        }
        isPaused(subscription) -> {
            res.getString(R.string.subscription_option_premium_message_account_paused)
        }
        isGracePeriod(subscription) -> {
            res.getString(R.string.subscription_option_premium_message_grace_period)
        }
        isSubscriptionRestore(subscription) -> {
            res.getString(R.string.subscription_option_premium_message_restore)
        }
        isPremiumContent(subscription) -> {
            res.getString(R.string.subscription_option_premium_message_current)
        }
        else -> {
            res.getString(R.string.subscription_option_premium_message)
        }
    }
    return if (subscription.isLocalPurchase) {
        text
    } else {
        // No local record, so the subscription cannot be managed on this device.
        "$text*"
    }
}
