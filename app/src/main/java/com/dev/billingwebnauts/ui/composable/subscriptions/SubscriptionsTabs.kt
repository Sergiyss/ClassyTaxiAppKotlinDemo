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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.dev.billingwebnauts.R
import com.dev.billingwebnauts.data.ContentResource
import com.dev.billingwebnauts.ui.BillingViewModel
import com.dev.billingwebnauts.ui.SubscriptionStatusViewModel
import com.dev.billingwebnauts.ui.composable.SelectedSubscriptionTab

@Composable
fun SubscriptionScreen(
    billingViewModel: BillingViewModel,
    subscriptionStatusViewModel: SubscriptionStatusViewModel,
    modifier: Modifier = Modifier,
) {
    val (selectedTab, setSelectedTab) = remember {
        mutableStateOf(
            SelectedSubscriptionTab.BASIC.index
        )
    }

    val currentSubscription by subscriptionStatusViewModel.currentSubscription.collectAsState(
        initial = SubscriptionStatusViewModel.CurrentSubscription.NONE
    )

    val premiumContentResource by subscriptionStatusViewModel.premiumContent.collectAsState(
        initial = ContentResource("google.com")
    )

    val basicContentResource by subscriptionStatusViewModel.basicContent.collectAsState(
        initial = ContentResource("google.com")
    )

    Surface(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == SelectedSubscriptionTab.BASIC.index,
                    onClick = { setSelectedTab(SelectedSubscriptionTab.BASIC.index) },
                    text = {
                        Text(
                            text = stringResource(id = R.string.tab_text_home),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
                Tab(
                    selected = selectedTab == SelectedSubscriptionTab.PREMIUM.index,
                    onClick = { setSelectedTab(SelectedSubscriptionTab.PREMIUM.index) },
                    text = {
                        Text(
                            text = stringResource(id = R.string.tab_text_premium),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )

                Tab(
                    selected = selectedTab == SelectedSubscriptionTab.SETTINGS.index,
                    onClick = { setSelectedTab(SelectedSubscriptionTab.SETTINGS.index) },
                    text = {
                        Text(
                            text = stringResource(id = R.string.tab_text_settings),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }

            when (selectedTab) {
                SelectedSubscriptionTab.BASIC.index -> BasicTabScreens(
                    billingViewModel = billingViewModel,
                    currentSubscription = currentSubscription,
                    contentResource = basicContentResource
                )

                SelectedSubscriptionTab.PREMIUM.index -> PremiumTabScreens(
                    billingViewModel = billingViewModel,
                    currentSubscription = currentSubscription,
                    contentResource = premiumContentResource
                )

                SelectedSubscriptionTab.SETTINGS.index -> SubscriptionSettingsScreen(
                    billingViewModel = billingViewModel
                )
            }
        }
    }
}