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

package com.dev.billingwebnauts.gpbl

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.dev.billingwebnauts.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow

class BillingClientLifecycle private constructor(
    private val applicationContext: Context,
    private val externalScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : DefaultLifecycleObserver, PurchasesUpdatedListener, BillingClientStateListener,
    ProductDetailsResponseListener, PurchasesResponseListener {

    private val _subscriptionPurchases = MutableStateFlow<List<Purchase>>(emptyList())
    private val _oneTimeProductPurchases = MutableStateFlow<List<Purchase>>(emptyList())

    /**
     * Purchases are collectable. This list will be updated when the Billing Library
     * detects new or existing purchases.
     */
    val subscriptionPurchases = _subscriptionPurchases.asStateFlow()

    val oneTimeProductPurchases = _oneTimeProductPurchases.asStateFlow()

    /**
     * Cached in-app product purchases details.
     */
    private var cachedPurchasesList: List<Purchase>? = null

    /**
     * ProductDetails for all known products.
     */
    val premiumSubProductWithProductDetails = MutableLiveData<ProductDetails?>()

    val basicSubProductWithProductDetails = MutableLiveData<ProductDetails?>()

    val oneTimeProductWithProductDetails = MutableLiveData<ProductDetails?>()

    /**
     * Instantiate a new BillingClient instance.
     */
    private lateinit var billingClient: BillingClient

    override fun onCreate(owner: LifecycleOwner) {
        Log.d(TAG, "ON_CREATE")
        // Создаем новый BillingClient в onCreate().
        // Поскольку BillingClient можно использовать только один раз, нам нужно создать новый экземпляр
        // после завершения предыдущего подключения к Google Play Store в onDestroy().
        billingClient = BillingClient.newBuilder(applicationContext)
            .setListener(this)
            .enablePendingPurchases() // Not used for subscriptions.
            .build()
        if (!billingClient.isReady) {
            Log.d(TAG, "BillingClient: Начать соединение...")
            billingClient.startConnection(this)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Log.d(TAG, "ON_DESTROY")
        if (billingClient.isReady) {
            Log.d(TAG, "BillingClient можно использовать только один раз — закрытие соединения")
            // BillingClient можно использовать только один раз.
            // После вызова endConnection() мы должны создать новый BillingClient.
            billingClient.endConnection()
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        Log.d(TAG, "onBillingSetupFinished: $responseCode $debugMessage")
        if (responseCode == BillingClient.BillingResponseCode.OK) {
            // Биллинговый клиент готов.
            // Здесь вы можете запросить информацию о продуктах и ​​покупках.
            querySubscriptionProductDetails()
            queryOneTimeProductDetails()
            querySubscriptionPurchases()
            queryOneTimeProductPurchases()
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.d(TAG, "onBillingServiceDisconnected")
        // TODO: Try connecting again with exponential backoff.
        // billingClient.startConnection(this)
    }
    /**
     * Чтобы совершать покупки, вам потребуется [ProductDetails] для товара или подписки.
     * Это асинхронный вызов, результат которого будет получен в [onProductDetailsResponse].
     *
     * querySubscriptionProductDetails использует вызовы методов из GPBL 5.0.0. PBL5, выпущенный в мае 2022 г.,
     * обратно совместим с предыдущими версиями.
     * Чтобы узнать больше об этом, вы можете прочитать:
     * https://developer.android.com/google/play/billing/compatibility.
     */
    private fun querySubscriptionProductDetails() {
        Log.d(TAG, "querySubscriptionProductDetails")
        val params = QueryProductDetailsParams.newBuilder()

        val productList: MutableList<QueryProductDetailsParams.Product> = arrayListOf()
        for (product in LIST_OF_SUBSCRIPTION_PRODUCTS) {
            productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(product)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
        }

        params.setProductList(productList).let { productDetailsParams ->
            billingClient.queryProductDetailsAsync(productDetailsParams.build(), this)
        }

    }

    /**
     * Для совершения покупок вам необходим [ProductDetails] для одноразового товара.
     * Это асинхронный вызов, результат которого будет получен в [onProductDetailsResponse].
     *
     * queryOneTimeProductDetails использует вызовы метода [BillingClient.queryProductDetailsAsync].
     * из ГПБЛ 5.0.0. PBL5, выпущенный в мае 2022 года, обратно совместим с предыдущими версиями.
     * Чтобы узнать больше об этом, вы можете прочитать:
     * https://developer.android.com/google/play/billing/compatibility.
     */
    private fun queryOneTimeProductDetails() {
        Log.d(TAG, "queryOneTimeProductDetails")
        val params = QueryProductDetailsParams.newBuilder()

        val productList = LIST_OF_ONE_TIME_PRODUCTS.map { product ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(product)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        params.apply {
            setProductList(productList)
        }.let { productDetailsParams ->
            billingClient.queryProductDetailsAsync(productDetailsParams.build(), this)
        }
    }

    /**
     * Получает результат от [querySubscriptionProductDetails].
     *
     * Сохраните ProductDetails и опубликуйте их в [basicSubProductWithProductDetails] и
     * [premiumSubProductWithProductDetails]. Это позволяет другим частям приложения использовать
     * [ProductDetails] для отображения информации о продукте и совершения покупок.
     *
     * onProductDetailsResponse() использует вызовы методов из GPBL 5.0.0. PBL5, выпущенный в мае 2022 г.,
     * обратно совместим с предыдущими версиями.
     * Чтобы узнать больше об этом, вы можете прочитать:
     * https://developer.android.com/google/play/billing/compatibility.
     */
    override fun onProductDetailsResponse(
        billingResult: BillingResult,
        productDetailsList: MutableList<ProductDetails>
    ) {
        val response = BillingResponse(billingResult.responseCode)
        val debugMessage = billingResult.debugMessage
        when {
            response.isOk -> {
                println("product detalis -1 => $productDetailsList")
                if(productDetailsList.isNotEmpty()) {
                    processProductDetails(productDetailsList)
                }
            }

            response.isTerribleFailure -> {
                // These response codes are not expected.
                Log.w(
                    TAG,
                    "onProductDetailsResponse - Unexpected error: ${response.code} $debugMessage"
                )
            }

            else -> {
                Log.e(TAG, "onProductDetailsResponse: ${response.code} $debugMessage")
            }

        }
    }

    /**
     * Этот метод используется для обработки списка сведений о продукте, возвращаемого [BillingClient] и
     * опубликуйте детали в [basicSubProductWithProductDetails] и
     * [premiumSubProductWithProductDetails] актуальные данные.
     *
     * @param productDetailsList Список сведений о продукте.
     *
     */
    private fun processProductDetails(productDetailsList: MutableList<ProductDetails>) {

        println("product detalis 0 => $productDetailsList   ${productDetailsList.size}")
        val expectedProductDetailsCount = LIST_OF_SUBSCRIPTION_PRODUCTS.size
        if (productDetailsList.isEmpty()) {
            Log.e(
                TAG, "processProductDetails: " +
                        "Expected ${expectedProductDetailsCount}, " +
                        "Found null ProductDetails. " +
                        "Check to see if the products you requested are correctly published " +
                        "in the Google Play Console."
            )
            postProductDetails(emptyList())
        } else {
            postProductDetails(productDetailsList)
        }
    }

    /**
     * Цей метод використовується для публікації деталей продукту в [basicSubProductWithProductDetails]
     * and [premiumSubProductWithProductDetails] live data.
     *
     * @param productDetailsList The list of product details.
     *
     */
    private fun postProductDetails(productDetailsList: List<ProductDetails>) {
        productDetailsList.forEach { productDetails ->
            println("product detalis 1 => $productDetails")
            when (productDetails.productType) {
                BillingClient.ProductType.SUBS -> {
                    if (productDetails.productId == Constants.PREMIUM_PRODUCT) {
                        println("product detalis 2 => $productDetails")
                        premiumSubProductWithProductDetails.postValue(productDetails)
                    } else if (productDetails.productId == Constants.BASIC_PRODUCT) {
                        println("product detalis 3 => $productDetails")
                        basicSubProductWithProductDetails.postValue(productDetails)
                    }
                }

                BillingClient.ProductType.INAPP -> {
                    if (productDetails.productId == Constants.ONE_TIME_PRODUCT) {
                        oneTimeProductWithProductDetails.postValue(productDetails)
                    }
                }
            }
        }
    }

    /**
     * Запросить Google Play Billing для существующих покупок подписки.
     *
     * Новые покупки будут передаваться в PurchasesUpdatedListener.
     * Вам все равно необходимо проверить API биллинга Google Play, чтобы узнать, когда токены покупки будут удалены.
     */
    fun querySubscriptionPurchases() {
        if (!billingClient.isReady) {
            Log.e(TAG, "querySubscriptionPurchases: BillingClient is not ready")
            billingClient.startConnection(this)
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(), this
        )
    }

    /**
     * Запросите Google Play Billing для существующих разовых покупок продуктов.
     *
     * Новые покупки будут передаваться в PurchasesUpdatedListener.
     * Вам все равно необходимо проверить API биллинга Google Play, чтобы узнать, когда токены покупки будут удалены.
     */
    fun queryOneTimeProductPurchases() {
        if (!billingClient.isReady) {
            Log.e(TAG, "queryOneTimeProductPurchases: BillingClient не готов")
            billingClient.startConnection(this)
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(), this
        )
    }

    /**
     * Обратный вызов из библиотеки выставления счетов при вызове queryPurchasesAsync.
     */
    override fun onQueryPurchasesResponse(
        billingResult: BillingResult,
        purchasesList: MutableList<Purchase>
    ) {
        processPurchases(purchasesList)
    }

    /**
     * Вызывается Библиотекой выставления счетов при обнаружении новых покупок.
     */
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        Log.d(TAG, "onPurchasesUpdated: $responseCode $debugMessage")
        when (responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases == null) {
                    Log.d(TAG, "onPurchasesUpdated: нулевой список покупок")
                    processPurchases(null)
                } else {
                    //Метод подтверждения новой покупки на строне сервера
                    //processPurchases(purchases)
                    //Метод подтверждения новой покупки на строне клиента
                    acknowledgePurchases(purchases.get(0))
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "onPurchasesUpdated: пользователь отменил покупку")
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.i(TAG, "onPurchasesUpdated: пользователь уже владеет этим предметом.")
            }

            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                Log.e(
                    TAG, "onPurchasesUpdated: ошибка разработчика означает, что Google Play делает " +
                            "не распознать конфигурацию. Если вы только начинаете\", +\n" +
                            "\"убедитесь, что вы правильно настроили приложение в разделе \" +\n" +
                            "«Консоль Google Play. Идентификатор продукта должен совпадать с вашим APK» +\n" +
                            "«используемые файлы должны быть подписаны ключами выпуска."
                )
            }
        }
    }

    /**
     * Выполняем подтверждение покупки новой подписки на стороне клиента.
     * **/
    private fun acknowledgePurchases(purchase: Purchase?) {
        purchase?.let {
            if (!it.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(it.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(
                    params
                ) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                    ) {
                        println(">>>>>>>>>> есть подписки .....")
                    }
                }
            }
        }
    }

    /**
     * Отправьте покупку в StateFlow, что вызовет сетевой вызов для проверки подписок.
     * на сервере.
     */
    private fun processPurchases(purchasesList: List<Purchase>?) {
        Log.d(TAG, "processPurchases: ${purchasesList?.size} purchase(s)")
        purchasesList?.let { list ->
            if (isUnchangedPurchaseList(list)) {
                Log.d(TAG, "processPurchases: список покупок не изменился")
                return
            }
            externalScope.launch {
                val subscriptionPurchaseList = list.filter { purchase ->
                    purchase.products.any { product ->
                        product in listOf(Constants.PREMIUM_PRODUCT, Constants.BASIC_PRODUCT)
                    }
                }

                val oneTimeProductPurchaseList = list.filter { purchase ->
                    purchase.products.contains(Constants.ONE_TIME_PRODUCT)
                }


                println("ПОДТВЕРЖДЕНИЕ СЕРВЕРОМ ПОКУПКУ ${subscriptionPurchaseList.lastOrNull().toString()} -- ${oneTimeProductPurchaseList.lastOrNull().toString()}")

                _oneTimeProductPurchases.emit(oneTimeProductPurchaseList)
                _subscriptionPurchases.emit(subscriptionPurchaseList)
            }
            logAcknowledgementStatus(list)
        }
    }

    /**
     * Проверьте, изменились ли покупки, прежде чем публиковать изменения.
     */
    private fun isUnchangedPurchaseList(purchasesList: List<Purchase>): Boolean {
        val isUnchanged = purchasesList == cachedPurchasesList
        if (!isUnchanged) {
            cachedPurchasesList = purchasesList
        }
        return isUnchanged
    }

    /**
     * Зарегистрируйте количество подтвержденных и неподтвержденных покупок.
     *
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     *
     * При первом получении покупки она не будет подтверждена.
     * Это приложение отправляет токен покупки на сервер для регистрации. После
     * токен покупки зарегистрирован в учетной записи, приложение Android подтверждает токен покупки.
     * При следующем обновлении списка покупок он будет содержать подтвержденные покупки.
     */
    private fun logAcknowledgementStatus(purchasesList: List<Purchase>) {
        var acknowledgedCounter = 0
        var unacknowledgedCounter = 0
        for (purchase in purchasesList) {
            if (purchase.isAcknowledged) {
                acknowledgedCounter++
            } else {
                unacknowledgedCounter++
            }
        }
        Log.d(
            TAG,
            "logAcknowledgementStatus: acknowledged=$acknowledgedCounter " +
                    "unacknowledged=$unacknowledgedCounter"
        )
    }

    /**
     * Launching the billing flow.
     *
     * Launching the UI to make a purchase requires a reference to the Activity.
     */
    fun launchBillingFlow(activity: Activity, params: BillingFlowParams): Int {
        if (!billingClient.isReady) {
            Log.e(TAG, "launchBillingFlow: BillingClient is not ready")
        }
        val billingResult = billingClient.launchBillingFlow(activity, params)
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        Log.d(TAG, "launchBillingFlow: BillingResponse $responseCode $debugMessage")
        return responseCode
    }

    /**
     * Подтвердить покупку.
     *
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     *
     * Приложения должны подтвердить покупку после подтверждения того, что токен покупки
     * был связан с пользователем. Это приложение подтверждает покупки только после
     * успешное получение данных о подписке обратно с сервера.
     *
     * Разработчики могут подтверждать покупки с сервера, используя
     * API разработчика Google Play. Сервер имеет прямой доступ к базе данных пользователей,
     * поэтому использование API разработчика Google Play для подтверждения может быть более надежным.
     * TODO(134506821): Подтвердить покупки на сервере.
     * TODO: Удалить подтверждение покупки на стороне клиента после удаления связанных тестов.
     * Если токен покупки не будет подтвержден в течение 3 дней,
     *тогда Google Play автоматически вернет деньги и отзовет покупку.
     * Такое поведение помогает гарантировать, что с пользователей не будет взиматься плата за подписку, если только
     * пользователь успешно получил доступ к контенту.
     * Это устраняет категорию проблем, когда пользователи жалуются разработчикам.
     * что они заплатили за то, чего приложение им не дает.
     */
    suspend fun acknowledgePurchase(purchaseToken: String): Boolean {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        for (trial in 1..MAX_RETRY_ATTEMPT) {
            var response = BillingResponse(500)
            var bResult: BillingResult? = null
            billingClient.acknowledgePurchase(params) { billingResult ->
                response = BillingResponse(billingResult.responseCode)
                bResult = billingResult
            }

            when {
                response.isOk -> {
                    Log.i(TAG, "Подтверждение успеха – токен: $purchaseToken")
                    return true
                }

                response.canFailGracefully -> {
                    // Ignore the error
                    Log.i(TAG, "Токен $purchaseToken уже принадлежит")
                    return true
                }

                response.isRecoverableError -> {
                    // Retry to ack because these errors may be recoverable.
                    val duration = 500L * 2.0.pow(trial).toLong()
                    delay(duration)
                    if (trial < MAX_RETRY_ATTEMPT) {
                        Log.w(
                            TAG,
                            "Повторная попытка ($trial) подтвердить токен $purchaseToken - " +
                                    "code: ${bResult!!.responseCode}, message: " +
                                    bResult!!.debugMessage
                        )
                    }
                }

                response.isNonrecoverableError || response.isTerribleFailure -> {
                    Log.e(
                        TAG,
                        "Не удалось подтвердить токен $purchaseToken - " +
                                "code: ${bResult!!.responseCode}, message: " +
                                bResult!!.debugMessage
                    )
                    break
                }
            }
        }
        throw Exception("Не удалось подтвердить покупку!")
    }

    companion object {
        private const val TAG = "BillingLifecycle"
        private const val MAX_RETRY_ATTEMPT = 3

        private val LIST_OF_SUBSCRIPTION_PRODUCTS = listOf(
            Constants.BASIC_PRODUCT,
            Constants.PREMIUM_PRODUCT,
        )

        private val LIST_OF_ONE_TIME_PRODUCTS = listOf(
            Constants.ONE_TIME_PRODUCT,
        )

        @Volatile
        private var INSTANCE: BillingClientLifecycle? = null

        fun getInstance(applicationContext: Context): BillingClientLifecycle =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingClientLifecycle(applicationContext).also { INSTANCE = it }
            }
    }
}

@JvmInline
private value class BillingResponse(val code: Int) {
    val isOk: Boolean
        get() = code == BillingClient.BillingResponseCode.OK
    val canFailGracefully: Boolean
        get() = code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED
    val isRecoverableError: Boolean
        get() = code in setOf(
            BillingClient.BillingResponseCode.ERROR,
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        )
    val isNonrecoverableError: Boolean
        get() = code in setOf(
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.DEVELOPER_ERROR,
        )
    val isTerribleFailure: Boolean
        get() = code in setOf(
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED,
            BillingClient.BillingResponseCode.USER_CANCELED,
        )
}