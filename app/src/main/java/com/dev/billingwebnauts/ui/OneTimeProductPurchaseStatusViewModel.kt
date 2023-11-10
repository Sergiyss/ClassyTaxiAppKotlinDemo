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


package com.dev.billingwebnauts.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dev.billingwebnauts.BillingApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OneTimeProductPurchaseStatusViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _currentOneTimeProductPurchase =
        MutableStateFlow(CurrentOneTimeProductPurchase.NONE)
    val currentOneTimeProductPurchase = _currentOneTimeProductPurchase.asStateFlow()

    private val repository = (application as BillingApp).repository

    private val userCurrentOneTimeProduct = repository.hasOneTimeProduct

    val content = repository.otpContent

    // TODO show status in UI
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            userCurrentOneTimeProduct.collectLatest { hasCurrentOneTimeProduct ->
                when {
                    hasCurrentOneTimeProduct -> {
                        _currentOneTimeProductPurchase.value = CurrentOneTimeProductPurchase.OTP
                    }

                    !hasCurrentOneTimeProduct -> {
                        _currentOneTimeProductPurchase.value = CurrentOneTimeProductPurchase.NONE
                    }
                }
            }
        }
    }

    /**
     * Refresh the status of one-time product purchases.
     */
    fun manualRefresh() {

        viewModelScope.launch {
            repository.queryProducts()
        }

        viewModelScope.launch {
            val result = repository.fetchOneTimeProductPurchases()
            if (result.isFailure) {
                _errorMessage.emit(result.exceptionOrNull()?.localizedMessage)
            }
        }
    }

    enum class CurrentOneTimeProductPurchase {
        OTP,
        NONE;
    }

    companion object {
        private const val TAG = "OneTimeProductPurchaseViewModel"
    }
}