/*
 *
 *  * Copyright 2021 Google LLC. All rights reserved.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.dev.billingwebnauts

import com.google.firebase.messaging.FirebaseMessagingService

/**
 * [FcmRegistrationTokenService] implements [FirebaseMessagingService]
 * that listens for token refreshes and sends the updated one for FCM registration.
 *
 */
class FcmRegistrationTokenService : FirebaseMessagingService() {

    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        val app = application as BillingApp
        app.repository.registerInstanceId(token)
    }
}
