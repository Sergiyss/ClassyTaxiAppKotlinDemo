/*
 * Copyright 2019 Google LLC. All rights reserved.
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


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.dev.billingwebnauts.R

/**
 * Activity that displays QR codes that can be opened from a user's mobile phone
 * to manage their Google Play subscriptions.
 */
class TvManageSubscriptionsActivity : FragmentActivity() {

    companion object {

        private const val OPTION_GOOGLE_PLAY = 0
        private const val OPTION_BASIC_SUBSCRIPTION = 1
        private const val OPTION_PREMIUM_SUBSCRIPTION = 2
        private const val OPTION_DONE = 3

        private val OPTION_NAMES = intArrayOf(
            R.string.google_play_subscriptions,
            R.string.subscription_option_basic_message,
            R.string.subscription_option_premium_message
        )

        // Hard-coded QR Codes containing URLs for managing subscriptions in Google Play
        // QR1: https://play.google.com/store/account/subscriptions
        // QR2: https://play.google.com/store/account/subscriptions?product=basic_subscription&package=com.example.subscriptions"
        // QR3: https://play.google.com/store/account/subscriptions?product=premium_subscription&package=com.example.subscriptions"
        private val OPTION_DRAWABLES = intArrayOf(
            R.drawable.qr_code_playstore_subscription,
            R.drawable.qr_code_basic_subscription,
            R.drawable.qr_code_premium_subscription
        )

        /**
         * Helper function to create and add GuidedAction elements to the UI.
         */
        private fun addAction(
            context: Context?,
            actions: MutableList<GuidedAction>,
            id: Long,
            title: String,
            desc: String?
        ) {
            actions.add(
                GuidedAction.Builder(context)
                    .id(id)
                    .title(title)
                    .description(desc)
                    .build()
            )
        }
    }

    /**
     * Lifecycle call onCreate()
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (null == savedInstanceState) {
            GuidedStepSupportFragment.addAsRoot(
                this,
                TvManageSubscriptionsFragment(),
                android.R.id.content
            )
        }
    }

    /**
     * Fragment that presents a series of top level options to Manage Subscriptions by
     * leveraging Leanback's GuidedStepSupportFragment UI.
     */
    class TvManageSubscriptionsFragment : GuidedStepSupportFragment() {

        override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
            val title = getString(R.string.manage_subscription_label)
            val description = getString(R.string.manage_subscription_message)
            val icon = requireActivity().resources.getDrawable(R.drawable.tv_banner)
            return GuidanceStylist.Guidance(title, description, null, icon)
        }

        override fun onCreateActions(
            actions: MutableList<GuidedAction>,
            savedInstanceState: Bundle?
        ) {
            addAction(
                context,
                actions,
                OPTION_GOOGLE_PLAY.toLong(),
                resources.getString(R.string.google_play_subscriptions),
                null
            )
            addAction(
                context,
                actions,
                OPTION_BASIC_SUBSCRIPTION.toLong(),
                resources.getString(R.string.subscription_option_basic_message),
                null
            )
            addAction(
                context,
                actions,
                OPTION_PREMIUM_SUBSCRIPTION.toLong(),
                resources.getString(R.string.subscription_option_premium_message),
                null
            )
            addAction(
                context,
                actions,
                OPTION_DONE.toLong(),
                resources.getString(R.string.done),
                null
            )
        }

        override fun onGuidedActionClicked(action: GuidedAction?) {

            if (action != null) {
                if (action.id == OPTION_DONE.toLong()) {
                    requireActivity().finish()
                } else {
                    val fm = fragmentManager
                    val next = TvQRCodeViewerFragment.newInstance(selectedActionPosition)
                    add(fm, next)
                }
            }

        }

    }

    /**
     * Fragment that displays a QR code based on the previously selected subscription
     * by leveraging Leanback's GuidedStepSupportFragment UI.
     */
    class TvQRCodeViewerFragment : GuidedStepSupportFragment() {

        companion object {
            private const val ARG_SUBSCRIPTION_OPTION = "subscription.option"

            fun newInstance(option: Int): TvQRCodeViewerFragment {
                val f = TvQRCodeViewerFragment()
                val args = Bundle()
                args.putInt(ARG_SUBSCRIPTION_OPTION, option)
                f.arguments = args
                return f
            }
        }

        override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
            val selectedOption = arguments?.getInt(ARG_SUBSCRIPTION_OPTION)
            val title = getString(OPTION_NAMES[selectedOption!!])
            val description = getString(R.string.manage_subscription_scan_qr_code)
            val icon = requireActivity().resources.getDrawable(OPTION_DRAWABLES[selectedOption])
            return GuidanceStylist.Guidance(title, description, null, icon)
        }

        override fun onCreateActions(
            actions: MutableList<GuidedAction>,
            savedInstanceState: Bundle?
        ) {
            addAction(
                context,
                actions,
                OPTION_DONE.toLong(),
                getString(R.string.done),
                null
            )
        }

        override fun onGuidedActionClicked(action: GuidedAction?) {
            fragmentManager?.popBackStack()
        }
    }
}
