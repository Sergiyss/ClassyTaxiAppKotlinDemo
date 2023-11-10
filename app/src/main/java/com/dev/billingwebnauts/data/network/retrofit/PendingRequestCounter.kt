/*
 * Copyright 2021 Google LLC. All rights reserved.
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

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * Keep track of all pending network requests and set [StateFlow] "loading"
 * to true when there remaining pending requests and false when all requests have been responded to.
 *
 * TODO(cassigbe@): Improve Pending requests count according to b/199924571.
 */
class PendingRequestCounter {

    /**
     * Track the number of pending server requests.
     */
    private val pendingRequestCount = AtomicInteger()

    private val _loading = MutableStateFlow(false)
    /**
     * True when there are pending network requests.
     * This is used to show a progress bar in the UI.
     */
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /**
     * Executes the given [block] function while increase and decrease the counter.
     */
    suspend inline fun <T> use(block: () -> T): T {
        increment()
        try {
            return block()
        } finally {
            decrement()
        }
    }

    /**
     * Increment request count and update loading value.
     * Must plan on calling [decrement] when the request completes.
     */
    suspend fun increment() {
        val newCount = pendingRequestCount.incrementAndGet()
        Log.i(TAG, "Pending Server Requests: $newCount")
        if (newCount <= 0) {
            Log.e(TAG, "Unexpectedly low request count after new request: $newCount")
            _loading.emit(false)
        } else {
            _loading.emit(true)
        }
    }

    /**
     * Decrement request count and update loading value.
     * Must call [increment] each time a network call is made.
     * and call [decrement] when the server responds to the request.
     */
    suspend fun decrement() {
        val newCount = pendingRequestCount.decrementAndGet()
        Log.i(TAG, "Pending Server Requests: $newCount")
        if (newCount < 0) {
            Log.w(TAG, "Unexpectedly negative request count: $newCount")
            _loading.emit(false)
        } else if (newCount == 0) {
            _loading.emit(false)
        }
    }

    companion object {
        private const val TAG = "RequestCounter"
    }
}