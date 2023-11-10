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

package com.dev.billingwebnauts.presenter

import android.text.TextUtils
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import com.dev.billingwebnauts.data.subscriptions.SubscriptionContent

/**
 * Presenter class used to bind and display metadata from a SubscriptionContent object
 */
class SubscriptionDetailsPresenter : AbstractDetailsDescriptionPresenter() {

    override fun onBindDescription(viewHolder: ViewHolder, item: Any) {
        val subscription = item as SubscriptionContent

        viewHolder.title.text = subscription.title
        viewHolder.subtitle.text = subscription.subtitle
        if (!TextUtils.isEmpty(subscription.description)) {
            viewHolder.body.text = subscription.description
        }
    }
}
