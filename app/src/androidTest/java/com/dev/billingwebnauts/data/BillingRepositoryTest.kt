package com.dev.billingwebnauts.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.android.billingclient.api.Purchase
import com.dev.billingwebnauts.gpbl.BillingClientLifecycle
import com.dev.billingwebnauts.data.disk.BillingLocalDataSource
import com.dev.billingwebnauts.data.disk.db.SubscriptionPurchasesDatabase
import com.dev.billingwebnauts.data.network.BillingRemoteDataSource
import com.dev.billingwebnauts.data.network.firebase.FakeServerFunctions
import com.dev.billingwebnauts.data.network.firebase.ServerFunctions
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

fun samplePurchase(product: String, purchaseToken: String): Purchase =
    Purchase(
        """
        {"purchaseToken": "$purchaseToken", "productIds": ["$product"]}
        """, "signature"
    )

class BillingRepositoryTest {
    private lateinit var database: SubscriptionPurchasesDatabase
    private lateinit var serverFunctions: ServerFunctions
    private lateinit var billingLocalDataSource: BillingLocalDataSource
    private lateinit var billingRemoteDataSource: BillingRemoteDataSource
    private lateinit var billingClientLifecycle: BillingClientLifecycle
    private lateinit var repository: BillingRepository
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // TODO delegate the following instantiations to Hilt
        database = Room.inMemoryDatabaseBuilder(appContext, SubscriptionPurchasesDatabase::class.java).build()
        serverFunctions = FakeServerFunctions.getInstance()
        billingLocalDataSource = BillingLocalDataSource.getInstance(database.subscriptionStatusDao())
        billingRemoteDataSource = BillingRemoteDataSource.getInstance(serverFunctions)

        billingClientLifecycle = mockk()
        every { billingClientLifecycle.subscriptionPurchases } answers { MutableStateFlow(emptyList()) }

        testScope = TestScope(UnconfinedTestDispatcher())
        repository = BillingRepository.getInstance(
            billingLocalDataSource, billingRemoteDataSource, billingClientLifecycle, testScope
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun registerSubscription_callAcknowledge() {
        try {
            testScope.runTest(5_000) {
                // given
                val purchase = samplePurchase(
                    "basic_subscription",
                    "TEST_TOKEN"
                )
                coEvery {
                    billingClientLifecycle
                        .acknowledgePurchase("TEST_TOKEN")
                } returns true
                every {
                    billingClientLifecycle
                        .subscriptionPurchases
                } returns MutableStateFlow(listOf(purchase))

                // when
                val result = repository.registerSubscription(
                    "basic_subscription",
                    "TEST_TOKEN"
                )

                // then
                coVerify { billingClientLifecycle.acknowledgePurchase("TEST_TOKEN") }
                assertThat(result.isSuccess, `is`(true))

                // Walk around for timeout issue - likely error of Coroutines test
                cancel()
            }
        } catch (e: CancellationException) { /* ignore */
        }
    }

    @Test
    fun registerSubscription_failWhenAllAcknowledgeAttemptFailed() {
        try {
            testScope.runTest(5_000) {
                // given
                val purchase = samplePurchase(
                    "basic_subscription",
                    "TEST_TOKEN"
                )
                coEvery {
                    billingClientLifecycle
                        .acknowledgePurchase(any())
                } throws Exception("FAILURE!")
                every {
                    billingClientLifecycle
                        .subscriptionPurchases
                } returns MutableStateFlow(listOf(purchase))

                // when
                val result = repository.registerSubscription(
                    "basic_subscription",
                    "TEST_TOKEN"
                )

                // then
                assertThat(result.isFailure, `is`(true))
                assertThat(result.exceptionOrNull()?.message, `is`("FAILURE!"))

                // Walk around for timeout issue - likely error of Coroutines test
                cancel()
            }
        } catch (e: CancellationException) { /* ignore */
        }
    }

    @Test
    fun registerSubscription_emitRegisteredSubscriptionToStateFlow() {
        try {
            testScope.runTest(5_000) {
                // given
                val purchase = samplePurchase(
                    "basic_subscription",
                    "TEST_TOKEN"
                )
                coEvery {
                    billingClientLifecycle
                        .acknowledgePurchase("TEST_TOKEN")
                } coAnswers { true }
                val purchaseState = MutableStateFlow(listOf(purchase))
                every { billingClientLifecycle.subscriptionPurchases } returns purchaseState

                // when
                repository.registerSubscription(
                    "basic_subscription",
                    "TEST_TOKEN"
                )

                // then
                val storedSubscriptionList = billingLocalDataSource.getSubscriptions().first()
                val storedSubscription = storedSubscriptionList.first()
                assertThat(storedSubscription.product, `is`("basic_subscription"))
                assertThat(storedSubscription.purchaseToken, `is`("TEST_TOKEN"))

                // Walk around for timeout issue - likely error of Coroutines test
                cancel()
            }
        } catch (e: CancellationException) { /* ignore */
        }
    }
}