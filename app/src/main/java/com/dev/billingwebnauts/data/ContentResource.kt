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

package com.dev.billingwebnauts.data

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

data class ContentResource(
    val url: String?
) {
    companion object {
        private const val URL_KEY = "url"

        /**
         * Parse subscription data from String and return null if data is not valid.
         */
        fun listFromMap(map: Map<String, Any>): ContentResource? {
            val url = map[URL_KEY] as? String ?: return null
            return ContentResource(url)
        }

        /**
         * Parse subscription data from String and return null if data is not valid.
         */
        fun fromJsonString(dataString: String): ContentResource? {
            val gson = Gson()
            return try {
                gson.fromJson(dataString, ContentResource::class.java)
            } catch (e: JsonSyntaxException) {
                null
            }
        }
    }
}
