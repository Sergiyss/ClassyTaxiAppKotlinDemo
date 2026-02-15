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

package com.dev.billingwebnauts.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.dev.billingwebnauts.BillingApp
import com.dev.billingwebnauts.Constants
import com.dev.billingwebnauts.data.BillingRepository
import com.dev.billingwebnauts.data.otps.OneTimeProductPurchaseStatus
import com.dev.billingwebnauts.data.subscriptions.SubscriptionStatus
import com.dev.billingwebnauts.gpbl.deviceHasGooglePlaySubscription
import com.dev.billingwebnauts.gpbl.serverHasSubscription
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class BillingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BillingRepository = (application as BillingApp).repository

    /**
     * Local billing purchase data.
     */
    private val purchases = (application as BillingApp).billingClientLifecycle.subscriptionPurchases


    /**
     * Проверяет, имеет ли пользователь право на специальное предложение
     * В реальном приложении это должно проверяться на сервере
     */
    private suspend fun isUserEligibleForSpecialOffer(userId: String): Boolean {
        // Проверьте на вашем сервере, выполнил ли пользователь необходимые действия
        // Например: нажал кнопку "Получить спец предложение"
        return repository.checkSpecialOfferEligibility(userId)
    }

    /**
     * Покупка со специальным оффером
     */
    fun buySpecialOffer(userId: String) {
        viewModelScope.launch {
            if (!isUserEligibleForSpecialOffer(userId)) {
                Log.e(TAG, "User is not eligible for special offer")
                // Покажите сообщение пользователю
                return@launch
            }

            buyBasePlans(
                tag = Constants.SALE_BASIC_MONTHLY_PLAN,
                product = Constants.BASIC_PRODUCT,
                upDowngrade = false
            )
        }
    }

    /**
     * ProductDetails for all known Products.
     */
    private val premiumSubProductWithProductDetails =
        (application as BillingApp).billingClientLifecycle.premiumSubProductWithProductDetails

    private val basicSubProductWithProductDetails =
        (application as BillingApp).billingClientLifecycle.basicSubProductWithProductDetails

    private val oneTimeProductWithProductDetails =
        (application as BillingApp).billingClientLifecycle.oneTimeProductWithProductDetails

    /**
     * Subscriptions record according to the server.
     */
    private val subscriptions: StateFlow<List<SubscriptionStatus>> =
        (application as BillingApp).repository.subscriptions

    /**
     * One-time product purchases record according to the server.
     */
    val oneTimeProductPurchases: StateFlow<List<OneTimeProductPurchaseStatus>> =
        (application as BillingApp).repository.oneTimeProductPurchases

    /**
     * Send an event when the Activity needs to buy something.
     */
    val buyEvent = SingleLiveEvent<BillingFlowParams>()

    /**
     * Send an event when the UI should open the Google Play
     * Store for the user to manage their subscriptions.
     */
    val openPlayStoreSubscriptionsEvent = SingleLiveEvent<String>()

    /**
     * Open the Play Store subscription center. If the user has exactly one product,
     * then open the deeplink to the specific product.
     */
    fun openPlayStoreSubscriptions() {
        val hasBasic = deviceHasGooglePlaySubscription(purchases.value, Constants.BASIC_PRODUCT)
        val hasPremium = deviceHasGooglePlaySubscription(purchases.value, Constants.BASIC_PRODUCT)
        Log.d(TAG, "hasBasic: $hasBasic, hasPremium: $hasPremium")
        when {
            hasBasic && !hasPremium -> {
                // If we just have a basic subscription, open the basic Product.
                openPlayStoreSubscriptionsEvent.postValue(Constants.BASIC_PRODUCT)
            }

            !hasBasic && hasPremium -> {
                // If we just have a premium subscription, open the premium Product.
                openPlayStoreSubscriptionsEvent.postValue(Constants.PREMIUM_PRODUCT)
            }

            else -> {
                // If we do not have an active subscription,
                // or if we have multiple subscriptions, open the default subscription center.
                openPlayStoreSubscriptionsEvent.call()
            }
        }
    }

    /**
     * Open the subscription page on Google Play.
     *
     * Since the purchase tokens will not be returned during account hold or pause,
     * we use the server data to determine the deeplink to Google Play.
     */
    fun openSubscriptionPageOnGooglePlay() {
        subscriptions.value.let { subscriptionStatusList ->
            when {
                serverHasSubscription(subscriptionStatusList, Constants.PREMIUM_PRODUCT) ->
                    openProductPlayStoreSubscriptions(Constants.PREMIUM_PRODUCT)

                serverHasSubscription(subscriptionStatusList, Constants.BASIC_PRODUCT) ->
                    openProductPlayStoreSubscriptions(Constants.BASIC_PRODUCT)
            }
        }
    }

    /**
     * Open the Play Store for the product subscription.
     *
     * @param product Product Id of the subscription product.
     */
    private fun openProductPlayStoreSubscriptions(product: String) {
        openPlayStoreSubscriptionsEvent.postValue(product)
    }

    /**
     * BillingFlowParams Builder for normal purchases.
     *
     * @param productDetails ProductDetails object returned by the library.
     * @param offerToken the least priced offer's offer id token returned by
     * [leastPricedOfferToken].
     *
     * @return [BillingFlowParams] builder.
     */
    private fun billingFlowParamsBuilder(productDetails: ProductDetails, offerToken: String):
            BillingFlowParams {
        return BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        ).build()
    }

    /**
     * BillingFlowParams Builder for upgrades and downgrades.
     *
     * @param productDetails ProductDetails object returned by the library.
     * @param offerToken the least priced offer's offer id token returned by
     * [leastPricedOfferToken].
     * @param oldToken the purchase token of the subscription purchase being upgraded or downgraded.
     *
     * @return [BillingFlowParams] builder.
     */
    private fun upDowngradeBillingFlowParamsBuilder(
        productDetails: ProductDetails, offerToken: String, oldToken: String
    ): BillingFlowParams {
        return BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        ).setSubscriptionUpdateParams(
            BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                .setOldPurchaseToken(oldToken)
                .setSubscriptionReplacementMode(
                    BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE
                ).build()
        ).build()
    }


    /**
     * Calculates the lowest priced offer amongst all eligible offers.
     * In this implementation the lowest price of all offers' pricing phases is returned.
     * It's possible the logic can be implemented differently.
     * For example, the lowest average price in terms of month could be returned instead.
     *
     * @param offerDetails List of of eligible offers and base plans.
     *
     * @return the offer id token of the lowest priced offer.
     *
     */
    private fun leastPricedOfferToken(
        offerDetails: List<ProductDetails.SubscriptionOfferDetails>
    ): String {
        var offerToken = String()
        var leastPricedOffer: ProductDetails.SubscriptionOfferDetails
        var lowestPrice = Int.MAX_VALUE

        if (offerDetails.isNotEmpty()) {
            for (offer in offerDetails) {
                for (price in offer.pricingPhases.pricingPhaseList) {
                    if (price.priceAmountMicros < lowestPrice) {
                        lowestPrice = price.priceAmountMicros.toInt()
                        leastPricedOffer = offer
                        offerToken = leastPricedOffer.offerToken
                    }
                }
            }
        }
        return offerToken

        TODO("Replace this with least average priced offer implementation")
    }

    /**
     * Retrieves all eligible base plans and offers using tags from ProductDetails.
     *
     * @param offerDetails offerDetails from a ProductDetails returned by the library.
     * @param tag string representing tags associated with offers and base plans.
     *
     * @return the eligible offers and base plans in a list.
     *
     */
    private fun retrieveEligibleOffers(
        offerDetails: MutableList<ProductDetails.SubscriptionOfferDetails>, tag: String
    ):
            List<ProductDetails.SubscriptionOfferDetails> {
        val eligibleOffers = emptyList<ProductDetails.SubscriptionOfferDetails>().toMutableList()
        offerDetails.forEach { offerDetail ->
            if (offerDetail.offerTags.contains(tag)) {
                eligibleOffers.add(offerDetail)
            }
        }
        return eligibleOffers
    }

    /**
     * Для совершения покупки используйте платежную библиотеку Google Play.
     *
     * @param tag Строка, представляющая теги, связанные с предложениями и базовыми планами.
     * @param product Приобретаемый продукт.
     * @param upDowngrade Логическое значение, указывающее, является ли покупка повышением или понижением версии, и:
     * при переходе с одного базового плана на другой.
     *
     */
    fun buyBasePlans(tag: String, product: String, upDowngrade: Boolean) {
        val isProductOnServer = serverHasSubscription(subscriptions.value, product)
        val isProductOnDevice = deviceHasGooglePlaySubscription(purchases.value, product)
        Log.d(
            "Billing", "$product - isProductOnServer: $isProductOnServer," +
                    " isProductOnDevice: $isProductOnDevice"
        )

        when {
            isProductOnDevice && isProductOnServer -> {
                Log.d(
                    "Billing", "User is trying to top up prepaid subscription: $product. "
                )
            }

            isProductOnDevice && !isProductOnServer -> {
                Log.d(
                    "Billing", "The Google Play Billing Library APIs indicate that" +
                            "this Product is already owned, but the purchase token is not " +
                            "registered with the server. There might be an issue registering the " +
                            "purchase token, but it could also be that the user is converting " +
                            "the same product from one base plan to another. For example, " +
                            "converting from a monthly base plan to an annual base plan, or " +
                            "converting from a prepaid base plan to a monthly base plan. "
                )
            }

            !isProductOnDevice && isProductOnServer -> {
                Log.w(
                    "Billing", "WHOA! The server says that the user already owns " +
                            "this item: $product. This could be from another Google account. " +
                            "You should warn the user that they are trying to buy something " +
                            "from Google Play that they might already have access to from " +
                            "another purchase, possibly from a different Google account " +
                            "on another device.\n" +
                            "You can choose to block this purchase.\n" +
                            "If you are able to cancel the existing subscription on the server, " +
                            "you should allow the user to subscribe with Google Play, and then " +
                            "cancel the subscription after this new subscription is complete. " +
                            "This will allow the user to seamlessly transition their payment " +
                            "method from an existing payment method to this Google Play account."
                )
                return
            }
        }

        val basicSubProductDetails = basicSubProductWithProductDetails.value ?: run {
            Log.e(TAG, "Не вдалося знайти основні відомості про продукт. ${basicSubProductWithProductDetails.value} - $tag - $product   ")
            return
        }

        val basicOffers =
            basicSubProductDetails.subscriptionOfferDetails?.let { offerDetailsList ->
                retrieveEligibleOffers(
                    offerDetails = offerDetailsList,
                    tag = tag
                )
            }

        val premiumSubProductDetails = premiumSubProductWithProductDetails.value ?: run {
            Log.e(TAG, "Could not find Premium product details.")
            return
        }

        val premiumOffers =
            premiumSubProductDetails.subscriptionOfferDetails?.let { offerDetailsList ->
                retrieveEligibleOffers(
                    offerDetails = offerDetailsList,
                    tag = tag
                )
            }

        val offerToken: String

        when (product) {
            Constants.BASIC_PRODUCT -> {
                offerToken = basicOffers?.let { leastPricedOfferToken(it) }.toString()
                launchFlow(upDowngrade, offerToken, basicSubProductDetails)
            }

            Constants.PREMIUM_PRODUCT -> {
                offerToken = premiumOffers?.let { leastPricedOfferToken(it) }.toString()
                launchFlow(upDowngrade, offerToken, premiumSubProductDetails)
            }
        }
    }

    /**
     * Launches the Billing Flow for a one-time product purchase.
     *
     */
    fun buyOneTimeProduct() {
        // First, the ProductDetails of the product being purchased.
        val productDetails =
            oneTimeProductWithProductDetails.value ?: run {
                Log.e(TAG, "Could not find ProductDetails to make purchase.")
                return
            }

        // Use [billingFlowParamsBuilder] to build the Params that describe the product to be
        // purchased and the offer to purchase with.
        val billingParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            )
        ).build()

        buyEvent.postValue(billingParams)
    }

    /**
     * Consumes a one-time product purchase.
     *
     * @param purchaseToken String representing the purchase token of the product being consumed.
     *
     */
    suspend fun consumePurchase(purchaseToken: String) {
        val product = Constants.ONE_TIME_PRODUCT

        repository.consumeOneTimeProductPurchase(product = product, purchaseToken = purchaseToken)
    }

    /**
     * Launches the billing flow for a subscription product purchase.
     * A user can only have one subscription purchase on the device at a time. If the user
     * has more than one subscription purchase on the device, the app should not allow the
     * user to purchase another subscription.
     *
     * @param upDowngrade Boolean indicating if the purchase is an upgrade or downgrade and
     * when converting from one base plan to another.
     * @param offerToken String representing the offer token of the lowest priced offer.
     * @param productDetails ProductDetails of the product being purchased.
     *
     */
    private fun launchFlow(
        upDowngrade: Boolean,
        offerToken: String,
        productDetails: ProductDetails
    ) {

        val currentSubscriptionPurchaseCount = purchases.value.count {
            it.products.contains(Constants.BASIC_PRODUCT) ||
                    it.products.contains(Constants.PREMIUM_PRODUCT)
        }

        if (currentSubscriptionPurchaseCount > EXPECTED_SUBSCRIPTION_PURCHASE_LIST_SIZE) {
            Log.e(TAG, "There are more than one subscription purchases on the device.")
            return

            TODO(
                "Handle this case better, such as by showing a dialog to the user or by " +
                        "programmatically getting the correct purchase token."
            )
        }

        val oldToken = purchases.value.filter {
            it.products.contains(Constants.BASIC_PRODUCT) ||
                    it.products.contains(Constants.PREMIUM_PRODUCT)
        }.firstOrNull { it.purchaseToken.isNotEmpty() }?.purchaseToken ?: ""


        val billingParams: BillingFlowParams = if (upDowngrade) {
            upDowngradeBillingFlowParamsBuilder(
                productDetails = productDetails,
                offerToken = offerToken,
                oldToken = oldToken
            )
        } else {
            billingFlowParamsBuilder(
                productDetails = productDetails,
                offerToken = offerToken
            )
        }
        buyEvent.postValue(billingParams)
    }


    suspend fun registerPurchases(purchases: List<Purchase>) {
        if (purchases.isEmpty()) {
            Log.e(TAG, "No purchases to register.")
        } else {
            repository.registerPurchases(purchases)
        }
    }

    companion object {
        const val TAG = "BillingViewModel"
        const val EXPECTED_SUBSCRIPTION_PURCHASE_LIST_SIZE = 1
    }
}
