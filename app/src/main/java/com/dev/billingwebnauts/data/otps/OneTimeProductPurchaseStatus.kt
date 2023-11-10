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


package com.dev.billingwebnauts.data.otps

import androidx.room.PrimaryKey

@androidx.room.Entity(tableName = "oneTimeProductPurchases")
data class OneTimeProductPurchaseStatus(
    // Local fields.
    @PrimaryKey(autoGenerate = true)
    var primaryKey: Int = 0,
    var isLocalPurchase: Boolean = false,
    var isAlreadyOwned: Boolean = false,

    // Remote fields.
    var product: String? = null,
    var purchaseToken: String? = null,
    var isEntitlementActive: Boolean = false,
    var isAcknowledged: Boolean = false,
    var isConsumed: Boolean = false,
    var quantity: Int = 0
)
