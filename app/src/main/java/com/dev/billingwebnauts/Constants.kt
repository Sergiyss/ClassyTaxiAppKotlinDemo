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

package com.dev.billingwebnauts

object Constants {

        // Use the fake local server data or real remote server.
        @Volatile
        var USE_FAKE_SERVER = true

        //Product IDs
        const val BASIC_PRODUCT = "basic_subscription"
        const val PREMIUM_PRODUCT = "premium_subscription"
        const val ONE_TIME_PRODUCT = "otp"

        //Tags
        const val BASIC_MONTHLY_PLAN = "basicmonthly_1"
        const val BASIC_YEARLY_PLAN = "basicyearly_1"
        const val PREMIUM_MONTHLY_PLAN = "premiummonthly"
        const val PREMIUM_YEARLY_PLAN = "premiumyearly"
        const val BASIC_PREPAID_PLAN_TAG = "prepaidbasic"
        const val PREMIUM_PREPAID_PLAN_TAG = "prepaidpremium"


        const val PLAY_STORE_SUBSCRIPTION_URL
                = "https://play.google.com/store/account/subscriptions"
        const val PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL
                = "https://play.google.com/store/account/subscriptions?product=%s&package=%s"

}
