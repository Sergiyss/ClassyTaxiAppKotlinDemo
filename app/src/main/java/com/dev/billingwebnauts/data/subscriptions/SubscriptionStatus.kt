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

package com.dev.billingwebnauts.data.subscriptions

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local subscription data. This is stored on disk in a database.
 */
@Entity(tableName = "subscriptions")
data class SubscriptionStatus(
    // Local fields.
    @PrimaryKey(autoGenerate = true)
    var primaryKey: Int = 0,
    var subscriptionStatusJson: String? = null,
    var subAlreadyOwned: Boolean = false,
    var isLocalPurchase: Boolean = false,

    // Remote fields.
    var product: String? = null,
    var purchaseToken: String? = null,
    var isEntitlementActive: Boolean = false,
    var willRenew: Boolean = false,
    var activeUntilMillisec: Long = 0,
    var isGracePeriod: Boolean = false,
    var isAccountHold: Boolean = false,
    var isPaused: Boolean = false,
    var isAcknowledged: Boolean = false,
    var autoResumeTimeMillis: Long = 0
) {

    companion object {
        /**
         * Create a record for a subscription that is already owned by a different user.
         *
         * The server does not return JSON for a subscription that is already owned by
         * a different user, so we need to construct a local record with the basic fields.
         */
        fun alreadyOwnedSubscription(
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
    }

}


