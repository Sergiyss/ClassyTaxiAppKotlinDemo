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

import android.util.Log
import com.dev.billingwebnauts.BillingApp
import com.dev.billingwebnauts.data.subscriptions.SubscriptionStatusList
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BillingMessageService : FirebaseMessagingService() {
    private val gson: Gson = GsonBuilder().create()
    private val externalScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.data.let { data ->
            if (data.isNotEmpty()) {
                var result: SubscriptionStatusList? = null
                if (REMOTE_MESSAGE_SUBSCRIPTIONS_KEY in data) {
                    result = gson.fromJson(
                        data[REMOTE_MESSAGE_SUBSCRIPTIONS_KEY],
                        SubscriptionStatusList::class.java
                    )
                }
                if (result == null) {
                    Log.e(TAG, "Invalid subscription data")
                } else {
                    Log.i(TAG, "onMessageReceived - ${result.subscriptions} ")
                    val app = application as BillingApp
                    externalScope.launch {
                        try {
                            app.repository.updateSubscriptionsFromNetwork(result.subscriptions)
                        } catch (e: Exception) {
                            Log.e(TAG, e.localizedMessage ?: "Failed to update subscription!")
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = BillingMessageService::class.java.simpleName
        private const val REMOTE_MESSAGE_SUBSCRIPTIONS_KEY = "currentStatus"
    }
}