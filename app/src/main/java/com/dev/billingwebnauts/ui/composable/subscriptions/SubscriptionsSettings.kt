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


package com.dev.billingwebnauts.ui.composable.subscriptions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.dev.billingwebnauts.R
import com.dev.billingwebnauts.ui.BillingViewModel
import com.dev.billingwebnauts.ui.composable.ClassyTaxiScreenHeader
import com.dev.billingwebnauts.ui.composable.resetSelectedButton

@Composable
fun SubscriptionSettingsScreen(
    billingViewModel: BillingViewModel,
    modifier: Modifier = Modifier,
) {
    val selectedSettingsButton = remember { mutableStateOf(false) }

    Surface(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ClassyTaxiScreenHeader(
                content = {
                    SettingsButtons(
                        selectedSettingsButton = selectedSettingsButton
                    )
                },
                textResource = R.string.google_play_billing_settings_message
            )
        }
    }

    LaunchedEffect(key1 = selectedSettingsButton.value) {
        if (selectedSettingsButton.value) {
            billingViewModel.openPlayStoreSubscriptions()
            resetSelectedButton(
                selectedIntButton = null,
                selectedBooleanButton = selectedSettingsButton
            )
        }
    }

}

@Composable
private fun SettingsButtons(
    selectedSettingsButton: MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_height)))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { selectedSettingsButton.value = true }
        ) {
            Text(
                text = stringResource(R.string.tab_text_settings),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_height)))
    }
}