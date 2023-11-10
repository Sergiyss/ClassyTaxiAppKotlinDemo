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

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.dev.billingwebnauts.Constants
import com.dev.billingwebnauts.R
import com.dev.billingwebnauts.data.ContentResource
import com.dev.billingwebnauts.ui.BillingViewModel
import com.dev.billingwebnauts.ui.SubscriptionStatusViewModel
import com.dev.billingwebnauts.ui.composable.ClassyTaxiImage
import com.dev.billingwebnauts.ui.composable.ClassyTaxiScreenHeader
import com.dev.billingwebnauts.ui.composable.LoadingScreen
import com.dev.billingwebnauts.ui.composable.SelectedSubscriptionBasePlan
import com.dev.billingwebnauts.ui.composable.resetSelectedButton

@Composable
fun PremiumTabScreens(
    currentSubscription: SubscriptionStatusViewModel.CurrentSubscription,
    contentResource: ContentResource?,
    billingViewModel: BillingViewModel,
    modifier: Modifier = Modifier,
) {

    val selectedPremiumConversionButton =
        remember { mutableStateOf(SelectedSubscriptionBasePlan.NONE.index) }

    val selectedEntitlementButtonMonthly =
        remember { mutableStateOf(SelectedSubscriptionBasePlan.NONE.index) }

    val selectedEntitlementButtonPrepaid =
        remember { mutableStateOf(SelectedSubscriptionBasePlan.NONE.index) }

    when (currentSubscription) {
        SubscriptionStatusViewModel.CurrentSubscription.PREMIUM_PREPAID -> {
            if (contentResource != null) {
                PremiumEntitlementScreen(
                    contentResource = contentResource,
                    buttons = {
                        PremiumPrepaidEntitlementButtons(
                            selectedEntitlementButtonPrepaid = selectedEntitlementButtonPrepaid,
                        )
                    },
                    message = R.string.current_prepaid_premium_subscription_message,
                    currentPremiumPlan = Constants.PREMIUM_PREPAID_PLAN_TAG,
                    selectedButton = selectedEntitlementButtonPrepaid,
                    billingViewModel = billingViewModel
                )
            } else {
                LoadingScreen()
            }
        }

        SubscriptionStatusViewModel.CurrentSubscription.PREMIUM_RENEWABLE -> {
            if (contentResource != null) {
                PremiumEntitlementScreen(
                    contentResource = contentResource,
                    buttons = {
                        PremiumMonthlyEntitlementButtons(
                            selectedEntitlementButtonMonthly = selectedEntitlementButtonMonthly,
                        )
                    },
                    message = R.string.current_recurring_premium_subscription_message,
                    currentPremiumPlan = Constants.PREMIUM_MONTHLY_PLAN,
                    selectedButton = selectedEntitlementButtonMonthly,
                    billingViewModel = billingViewModel
                )
            } else {
                LoadingScreen()
            }
        }

        SubscriptionStatusViewModel.CurrentSubscription.BASIC_RENEWABLE -> {
            PremiumConversionScreen(
                billingViewModel = billingViewModel,
                buttons = {
                    PremiumUpgradeButtons(
                        selectedPremiumConversionButton = selectedPremiumConversionButton,
                    )
                },
                selectedButton = selectedPremiumConversionButton,
                message = R.string.current_recurring_basic_subscription_message,
            )
        }

        SubscriptionStatusViewModel.CurrentSubscription.BASIC_PREPAID -> {
            PremiumConversionScreen(
                billingViewModel = billingViewModel,
                buttons = {
                    PremiumUpgradeButtons(
                        selectedPremiumConversionButton = selectedPremiumConversionButton,
                    )
                },
                selectedButton = selectedPremiumConversionButton,
                message = R.string.current_prepaid_basic_subscription_message,
            )
        }

        SubscriptionStatusViewModel.CurrentSubscription.NONE -> {
            PremiumBasePlansScreen(billingViewModel = billingViewModel)
        }
    }
}

@Composable
fun PremiumEntitlementScreen(
    contentResource: ContentResource,
    @StringRes message: Int,
    currentPremiumPlan: String,
    selectedButton: MutableState<Int>,
    billingViewModel: BillingViewModel,
    buttons: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ClassyTaxiImage(
            contentDescription = "Premium Entitlement",
            contentResource = contentResource
        )
        Surface {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ClassyTaxiScreenHeader(content = buttons, textResource = message)
            }
        }
    }

    LaunchedEffect(key1 = currentPremiumPlan, key2 = selectedButton.value) {
        when (currentPremiumPlan) {
            in listOf(Constants.PREMIUM_MONTHLY_PLAN, Constants.PREMIUM_YEARLY_PLAN) -> {
                if (selectedButton.value == SelectedSubscriptionBasePlan.PREPAID.index) {
                    billingViewModel.buyBasePlans(
                        product = Constants.PREMIUM_PRODUCT,
                        tag = Constants.PREMIUM_PREPAID_PLAN_TAG,
                        upDowngrade = true
                    )
                }
                resetSelectedButton(
                    selectedIntButton = selectedButton,
                    selectedBooleanButton = null
                )
            }

            Constants.PREMIUM_PREPAID_PLAN_TAG -> {
                when (selectedButton.value) {
                    SelectedSubscriptionBasePlan.MONTHLY.index -> {
                        billingViewModel.buyBasePlans(
                            product = Constants.PREMIUM_PRODUCT,
                            tag = Constants.PREMIUM_MONTHLY_PLAN,
                            upDowngrade = true
                        )
                        resetSelectedButton(
                            selectedIntButton = selectedButton,
                            selectedBooleanButton = null
                        )
                    }

                    SelectedSubscriptionBasePlan.YEARLY.index -> {
                        billingViewModel.buyBasePlans(
                            product = Constants.PREMIUM_PRODUCT,
                            tag = Constants.PREMIUM_YEARLY_PLAN,
                            upDowngrade = false
                        )
                        resetSelectedButton(
                            selectedIntButton = selectedButton,
                            selectedBooleanButton = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumConversionScreen(
    billingViewModel: BillingViewModel,
    selectedButton: MutableState<Int>,
    @StringRes message: Int,
    buttons: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ClassyTaxiScreenHeader(content = buttons, textResource = message)
        }
    }

    LaunchedEffect(key1 = selectedButton.value) {
        when (selectedButton.value) {
            SelectedSubscriptionBasePlan.MONTHLY.index -> {
                billingViewModel.buyBasePlans(
                    product = Constants.PREMIUM_PRODUCT,
                    tag = Constants.PREMIUM_MONTHLY_PLAN,
                    upDowngrade = true
                )
                resetSelectedButton(
                    selectedIntButton = selectedButton,
                    selectedBooleanButton = null
                )
            }

            SelectedSubscriptionBasePlan.YEARLY.index -> {
                billingViewModel.buyBasePlans(
                    product = Constants.PREMIUM_PRODUCT,
                    tag = Constants.PREMIUM_YEARLY_PLAN,
                    upDowngrade = true
                )
                resetSelectedButton(
                    selectedIntButton = selectedButton,
                    selectedBooleanButton = null
                )
            }

            SelectedSubscriptionBasePlan.PREPAID.index -> {
                billingViewModel.buyBasePlans(
                    product = Constants.PREMIUM_PRODUCT,
                    tag = Constants.PREMIUM_PREPAID_PLAN_TAG,
                    upDowngrade = true
                )
                resetSelectedButton(
                    selectedIntButton = selectedButton,
                    selectedBooleanButton = null
                )
            }
        }
    }
}

@Composable
fun PremiumBasePlansScreen(
    billingViewModel: BillingViewModel,
    modifier: Modifier = Modifier,
) {
    val selectedPremiumSubscriptionButton = remember {
        mutableStateOf(SelectedSubscriptionBasePlan.NONE.index)
    }

    Surface(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ClassyTaxiScreenHeader(
                content = {
                    PremiumBasePlansButtons(
                        selectedPremiumSubscriptionButton = selectedPremiumSubscriptionButton
                    )
                },
                textResource = R.string.premium_paywall_message
            )
        }
    }

    LaunchedEffect(key1 = selectedPremiumSubscriptionButton.value) {
        when (selectedPremiumSubscriptionButton.value) {
            SelectedSubscriptionBasePlan.MONTHLY.index -> {
                billingViewModel.buyBasePlans(
                    tag = Constants.PREMIUM_MONTHLY_PLAN,
                    product = Constants.PREMIUM_PRODUCT,
                    upDowngrade = false
                )
                resetSelectedButton(
                    selectedIntButton = selectedPremiumSubscriptionButton,
                    selectedBooleanButton = null
                )
            }

            SelectedSubscriptionBasePlan.YEARLY.index -> {
                billingViewModel.buyBasePlans(
                    product = Constants.PREMIUM_PRODUCT,
                    tag = Constants.PREMIUM_YEARLY_PLAN,
                    upDowngrade = false
                )
                resetSelectedButton(
                    selectedIntButton = selectedPremiumSubscriptionButton,
                    selectedBooleanButton = null
                )
            }

            SelectedSubscriptionBasePlan.PREPAID.index -> {
                billingViewModel.buyBasePlans(
                    product = Constants.PREMIUM_PRODUCT,
                    tag = Constants.PREMIUM_PREPAID_PLAN_TAG,
                    upDowngrade = false
                )
                resetSelectedButton(
                    selectedIntButton = selectedPremiumSubscriptionButton,
                    selectedBooleanButton = null
                )
            }
        }
    }
}

@Composable
private fun PremiumBasePlansButtons(
    selectedPremiumSubscriptionButton: MutableState<Int>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Button(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.menu_drawer_body_button_padding)),
            onClick = {
                selectedPremiumSubscriptionButton.value = SelectedSubscriptionBasePlan.MONTHLY.index
            }) {
            Text(
                text = stringResource(id = R.string.monthly_premium_plan_text),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.menu_drawer_body_button_padding)),
            onClick = {
                selectedPremiumSubscriptionButton.value = SelectedSubscriptionBasePlan.YEARLY.index
            }) {
            Text(
                text = stringResource(id = R.string.yearly_premium_plan_text),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.menu_drawer_body_button_padding)),
            onClick = {
                selectedPremiumSubscriptionButton.value = SelectedSubscriptionBasePlan.PREPAID.index
            }) {
            Text(
                text = stringResource(id = R.string.prepaid_premium_plan_text),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun PremiumPrepaidEntitlementButtons(
    selectedEntitlementButtonPrepaid: MutableState<Int>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.menu_drawer_body_button_padding)),
            onClick = {
                selectedEntitlementButtonPrepaid.value = SelectedSubscriptionBasePlan.MONTHLY.index
            }) {
            Text(
                text = stringResource(id = R.string.convert_to_premium_monthly),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.menu_drawer_body_button_padding)),
            onClick = {
                selectedEntitlementButtonPrepaid.value = SelectedSubscriptionBasePlan.YEARLY.index
            }) {
            Text(
                text = stringResource(id = R.string.convert_to_premium_yearly),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun PremiumMonthlyEntitlementButtons(
    selectedEntitlementButtonMonthly: MutableState<Int>,
    modifier: Modifier = Modifier,
) {
    Column {
        Button(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.menu_drawer_body_button_padding)),
            onClick = {
                selectedEntitlementButtonMonthly.value = SelectedSubscriptionBasePlan.PREPAID.index
            }) {
            Text(
                text = stringResource(id = R.string.convert_to_premium_prepaid),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun PremiumUpgradeButtons(
    selectedPremiumConversionButton: MutableState<Int>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.menu_drawer_body_button_padding)),
            onClick = {
                selectedPremiumConversionButton.value = SelectedSubscriptionBasePlan.MONTHLY.index
            }) {
            Text(
                text = stringResource(id = R.string.upgrade_to_premium_monthly),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.menu_drawer_body_button_padding)),
            onClick = {
                selectedPremiumConversionButton.value = SelectedSubscriptionBasePlan.YEARLY.index
            }) {
            Text(
                text = stringResource(id = R.string.upgrade_to_premium_yearly),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.menu_drawer_body_button_padding)),
            onClick = {
                selectedPremiumConversionButton.value = SelectedSubscriptionBasePlan.PREPAID.index
            }) {
            Text(
                text = stringResource(id = R.string.upgrade_to_premium_prepaid),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}