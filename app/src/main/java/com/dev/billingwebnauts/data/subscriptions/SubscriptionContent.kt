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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * SubscriptionContent is an immutable object that holds the various metadata associated with a Subscription.
 */
@Parcelize
class SubscriptionContent(
    val title: String?,
    val subtitle: String?,
    val description: String? = null
) : Parcelable {
    // Builder for Subscription object.
    class Builder {
        private var title: String? = null
        private var subtitle: String? = null
        private var desc: String? = null

        fun title(title: String): Builder {
            this.title = title
            return this
        }

        fun subtitle(subtitle: String): Builder {
            this.subtitle = subtitle
            return this
        }

        fun description(desc: String?): Builder {
            this.desc = desc
            return this
        }

        fun build(): SubscriptionContent {
            return SubscriptionContent(title, subtitle, desc)
        }
    }
}
