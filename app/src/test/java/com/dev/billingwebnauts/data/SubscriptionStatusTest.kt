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

import com.dev.billingwebnauts.data.subscriptions.SubscriptionStatus
import com.dev.billingwebnauts.data.subscriptions.SubscriptionStatusList
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader

class SubscriptionStatusTest {
    private val gson: Gson = GsonBuilder().create()

    @Test
    fun alreadyOwnedIsNotValid() {
        assertEquals(alreadyOwnedSub.product, "TEST_PRODUCT")
        assertEquals(alreadyOwnedSub.purchaseToken, "TEST_PURCHASE_TOKEN")
        assertFalse("Sub must not be active", alreadyOwnedSub.isEntitlementActive)
        assertTrue("Sub must be already owned", alreadyOwnedSub.subAlreadyOwned)
    }

    @Test
    fun parseEmptyFromJson() {
        val testData = readTestData("empty.json")
        val subList = gson.fromJson(testData, SubscriptionStatusList::class.java)
        assertNotNull("List must not be null", subList.subscriptions)
        if (subList == null) return
        assertTrue(
            "Empty data must create an empty list",
            subList.subscriptions.isNullOrEmpty()
        )
    }

    @Test
    fun doNotCrashWithUnrecognizedKeyFromJson() {
        val testData = readTestData("unrecognized.json")
        val subList = gson.fromJson(testData, SubscriptionStatusList::class.java)
        assertNull("Unrecognized data must return null", subList.subscriptions)
    }

    @Test
    fun parseBasicSubscriptionFromJson() {
        val testData = readTestData("basic.json")
        val subList = gson.fromJson(testData, SubscriptionStatusList::class.java)
        assertNotNull("List must not be null", subList.subscriptions)
        if (subList == null) return

        assertFalse(
            "Subscription must not be empty",
            subList.subscriptions.isNullOrEmpty()
        )
        subList.subscriptions!![0].run {
            assertTrue("Subscription must renew", willRenew)
            assertEquals(
                "Token must match", "onhhinbenecfbpohlgpkpica." +
                        "AO-J1OyHDwGfi22SvG2VdeGdrR9nz0D3WY_YPda6qr7yssmdQ6oX2PKEiKfaN4B9LVx1LJ" +
                        "SgkLVfuWbho2ReugyGThDgNy66a4EltFlLGVwZ4JK_CfTE5ypYz7E0SGuyO4wQNItR4hUP",
                purchaseToken
            )
            assertFalse("Grace period must match", isGracePeriod)
            assertTrue("Subscription must be active", isEntitlementActive)
            assertEquals(
                "Expiry time must match", 1523347054184L,
                activeUntilMillisec
            )
            assertEquals(
                "Subscription must be basic",
                "basic_subscription", product
            )
            assertFalse("Account hold must match", isAccountHold)
        }
    }

    @Test
    fun parseBasicSubscriptionFromJsonWithUnrecognizedKey() {
        val testData = readTestData("unrecognizedbasic.json")

        val subList = gson.fromJson(testData, SubscriptionStatusList::class.java)
        assertNotNull("List must not be null", subList)
        if (subList == null) return

        assertFalse(
            "Subscription must not be empty",
            subList.subscriptions.isNullOrEmpty()
        )
        subList.subscriptions!![0].run {
            assertTrue("Subscription must renew", willRenew)
            assertEquals(
                "Token must match", "onhhinbenecfbpohlgpkpica." +
                        "AO-J1OyHDwGfi22SvG2VdeGdrR9nz0D3WY_YPda6qr7yssmdQ6oX2PKEiKfaN4B9LVx1LJ" +
                        "SgkLVfuWbho2ReugyGThDgNy66a4EltFlLGVwZ4JK_CfTE5ypYz7E0SGuyO4wQNItR4hUP",
                purchaseToken
            )
            assertFalse("Grace period must match", isGracePeriod)
            assertTrue("Subscription must be active", isEntitlementActive)
            assertEquals(
                "Expiry time must match",
                1523347054184L, activeUntilMillisec
            )
            assertEquals(
                "Subscription must be basic",
                "basic_subscription", product
            )
            assertFalse("Account hold must match", isAccountHold)
        }
    }

    @Test
    fun parsePremiumSubscriptionFromJson() {
        val testData = readTestData("premium.json")
        val subList = gson.fromJson(testData, SubscriptionStatusList::class.java)
        assertNotNull("List must not be null", subList)
        if (subList == null) return

        assertFalse(
            "Subscription must not be empty",
            subList.subscriptions.isNullOrEmpty()
        )
        subList.subscriptions!![0].run {
            assertTrue("Subscription must renew", willRenew)
            assertEquals(
                "Token must match", "onhhinbenecfbpohlgpkpica." +
                        "AO-J1OyHDwGfi22SvG2VdeGdrR9nz0D3WY_YPda6qr7yssmdQ6oX2PKEiKfaN4B9LVx1LJ" +
                        "SgkLVfuWbho2ReugyGThDgNy66a4EltFlLGVwZ4JK_CfTE5ypYz7E0SGuyO4wQNItR4hUP",
                purchaseToken
            )
            assertFalse("Grace period must match", isGracePeriod)
            assertTrue("Subscription must be active", isEntitlementActive)
            assertEquals(
                "Expiry time must match",
                1523347054184L, activeUntilMillisec
            )
            assertEquals(
                "Subscription must be premium",
                "premium_subscription", product
            )
            assertFalse("Account hold must match", isAccountHold)
        }
    }

    @Test
    fun parseAccountHoldFromJson() {
        val testData = readTestData("accounthold.json")
        val subList = gson.fromJson(testData, SubscriptionStatusList::class.java)
        assertNotNull("List must not be null", subList)
        if (subList == null) return

        assertFalse(
            "Subscription must not be empty",
            subList.subscriptions.isNullOrEmpty()
        )
        subList.subscriptions!![0].run {
            assertEquals("Product must match", "basic_subscription", product)
            assertEquals(
                "Token must match", "jlbjaefklclbngobohijodfa." +
                        "AO-J1Ozh_NnSw4YWqcGNaVXBndXhUz1zS-6v_NDW6BaVTLCM9VOSfjLXTVgT2AlJdQD7" +
                        "TTxxe_kWV6U3mklXZgoJ7HxYEqtpkUtFsOfVjM0hf10tKXHZ1CPH4dVgGbCpFy2eI6ctYVgG",
                purchaseToken
            )
            assertFalse("Subscription must not be active", isEntitlementActive)
            assertTrue("Renewal must match", willRenew)
            assertEquals(
                "Expiry time must match",
                1523324641475L, activeUntilMillisec
            )
            assertFalse("Grace period must match", isGracePeriod)
            assertTrue("Account hold must match", isAccountHold)
        }
    }

    @Test
    fun parseGracePeriodFromJson() {
        val testData = readTestData("graceperiod.json")
        val subList = gson.fromJson(testData, SubscriptionStatusList::class.java)
        assertNotNull("List must not be null", subList)
        if (subList == null) return

        assertFalse(
            "Subscription must not be empty",
            subList.subscriptions.isNullOrEmpty()
        )
        subList.subscriptions!![0].run {
            assertTrue("Subscription must be active", isEntitlementActive)
            assertTrue("Subscription must be grace period", isGracePeriod)
        }
    }

    @Test
    fun parseCanceledSubscriptionFromJson() {
        val testData = readTestData("canceled.json")
        val subList = gson.fromJson(testData, SubscriptionStatusList::class.java)
        assertNotNull("List must not be null", subList)
        if (subList == null) return

        assertFalse(
            "Subscription must not be empty",
            subList.subscriptions.isNullOrEmpty()
        )
        subList.subscriptions!![0].run {
            assertEquals("Product must match", "cheap_subscription", product)
            assertEquals(
                "Token must match", "onhhinbenecfbpohlgpkpica." +
                        "AO-J1OyHDwGfi22SvG2VdeGdrR9nz0D3WY_YPda6qr7yssmdQ6oX2PKEiKfaN4B9LVx1L" +
                        "JSgkLVfuWbho2ReugyGThDgNy66a4EltFlLGVwZ4JK_CfTE5ypYz7E0SGuyO4wQNItR4hUP",
                purchaseToken
            )
            assertTrue("Subscription must be active", isEntitlementActive)
            assertFalse("Subscription must not renew", willRenew)
            assertEquals(
                "Expiry time must match",
                1523346934184L, activeUntilMillisec
            )
            assertFalse("Grace period must match", isGracePeriod)
            assertFalse("Account hold must match", isAccountHold)
        }
    }

    /**
     * Read JSON file data for testing.
     */
    private fun readTestData(filename: String): String {
        val inputStream = javaClass.getResourceAsStream(filename)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String?
        do {
            line = reader.readLine()
            stringBuilder.append(line ?: "")
        } while (line != null)
        return stringBuilder.toString()
    }

    companion object {
        val alreadyOwnedSub = SubscriptionStatus.alreadyOwnedSubscription(
            product = "TEST_PRODUCT",
            purchaseToken = "TEST_PURCHASE_TOKEN"
        )
    }
}
