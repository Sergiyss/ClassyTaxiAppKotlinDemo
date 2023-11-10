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

package com.dev.billingwebnauts.data.network.retrofit.authentication

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * An [Interceptor] that adds the Firebase Auth ID token to the retrofit request headers.
 */
class UserIdTokenInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val user = FirebaseAuth.getInstance().currentUser
        val request: Request = chain.request()
        if (user == null) {
            Log.i(TAG, "user is null")
            throw IOException("user is null")
        }
        val task = user.getIdToken(false)

        val result: GetTokenResult? = try {
            Tasks.await(task, 30000, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            throw IOException("Failed to get new user ID token", e)
        }
        val token = result?.token

        if (token == null) {
            Log.i(TAG, "token is null")
            throw IOException("token is null")
        } else {
            val modifiedRequest: Request = request.newBuilder()
                .addHeader(X_FIREBASE_ID_TOKEN, token)
                .build()
            return chain.proceed(modifiedRequest)
        }
    }

    companion object {
        private const val X_FIREBASE_ID_TOKEN = "X-FireIDToken"
        private const val TAG = "Interceptor"
    }
}