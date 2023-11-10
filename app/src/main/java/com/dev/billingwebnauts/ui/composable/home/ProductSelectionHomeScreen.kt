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


package com.dev.billingwebnauts.ui.composable.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.dev.billingwebnauts.R
import com.dev.billingwebnauts.ui.composable.ClassyTaxiScreenHeader

@Composable
fun ProductSelectionScreen(
    onNavigateToOneTimeProductScreen: () -> Unit,
    onNavigateToSubscriptionScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ClassyTaxiScreenHeader(
                content = { ProductSelectionButtons(
                    onNavigateToOneTimeProductScreen = onNavigateToOneTimeProductScreen,
                    onNavigateToSubscriptionScreen = onNavigateToSubscriptionScreen,
                ) },
                textResource = R.string.product_selection_screen_text
            )
        }
    }
}

@Composable
private fun ProductSelectionButtons(
    onNavigateToOneTimeProductScreen: () -> Unit,
    onNavigateToSubscriptionScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Button(
            onClick = { onNavigateToOneTimeProductScreen() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.menu_drawer_body_button_padding))
        ) {
            Text(text = stringResource(id = R.string.otp_selection_button_text))
        }
        Button(
            onClick = { onNavigateToSubscriptionScreen() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.menu_drawer_body_button_padding))
        ) {
            Text(text = stringResource(id = R.string.subscription_selection_button_text))
        }
    }
}