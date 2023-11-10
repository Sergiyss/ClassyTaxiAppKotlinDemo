package com.dev.billingwebnauts.ui.composable

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dev.billingwebnauts.R
import com.dev.billingwebnauts.data.ContentResource


enum class SelectedSubscriptionTab(val index: Int) {
    BASIC(0),
    PREMIUM(1),
    SETTINGS(2)
}

enum class SelectedSubscriptionBasePlan(val index: Int) {
    NONE(0),
    MONTHLY(1),
    YEARLY(2),
    PREPAID(3)
}

@Composable
private fun CircularProgressAnimated(modifier: Modifier = Modifier) {
    val progressValue = 1.0f
    val infiniteTransition = rememberInfiniteTransition(
        label = "Animated Progress"
    )

    val progressAnimationValue by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = progressValue,
        animationSpec = infiniteRepeatable(animation = tween(900)),
        label = "Animated Progress"
    )

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.progress_card_shape_size)),
                colors = CardDefaults.cardColors(Color.White),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = dimensionResource(id = R.dimen.image_display_card_elevation)
                )
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    progress = progressAnimationValue
                )
            }
        }
    }
}

@Composable
fun ClassyTaxiScreenHeader(
    textResource: Int,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.image_display_card_elevation)),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Column {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_height)))
            content()
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_height)))
            Text(
                text = stringResource(textResource),
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ClassyTaxiImage(
    contentDescription: String?,
    contentResource: ContentResource,
    modifier: Modifier = Modifier,
) {
    if (contentResource.url == null) {
        CircularProgressAnimated()
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.image_display_card_shape_size)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.image_display_card_elevation)),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.image_display_column_padding)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .height(dimensionResource(id = R.dimen.image_display_box_height)),
                contentAlignment = Alignment.Center
            ) {
                GlideImage(
                    model = contentResource.url,
                    contentDescription = contentDescription,
                )
            }
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressAnimated()
        }
    }
}

fun resetSelectedButton(
    selectedIntButton: MutableState<Int>?,
    selectedBooleanButton: MutableState<Boolean>?
) {
    selectedIntButton?.value = 0
    selectedBooleanButton?.value = false
}