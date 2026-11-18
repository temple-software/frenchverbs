package com.templesoftware.frenchverbs.ui.feedback

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.templesoftware.frenchverbs.ui.feedback.FeedbackEvent
import com.templesoftware.frenchverbs.ui.theme.educationalColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Shared correct-answer pattern; consumption is durable, animation is transient. */
@Composable
internal fun TempleSuccessStar(feedback: FeedbackEvent, claim: (Long) -> Boolean, modifier: Modifier, tag: String) {
    val alpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    var active by remember { mutableStateOf(false) }
    val latestClaim by rememberUpdatedState(claim)
    LaunchedEffect(feedback.id) {
        active = false
        alpha.snapTo(0f)
        rotation.snapTo(0f)
        if (feedback is FeedbackEvent.Correct && latestClaim(feedback.id)) {
            active = true
            try {
                coroutineScope {
                    launch { rotation.animateTo(360f, tween(850, easing = FastOutSlowInEasing)) }
                    launch {
                        alpha.animateTo(1f, tween(140))
                        delay(460)
                        alpha.animateTo(0f, tween(250))
                    }
                }
            } finally { active = false }
        }
    }
    if (active) Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.educationalColors.goldStar,
        modifier = modifier.size(60.dp).testTag(tag).graphicsLayer {
            this.alpha = alpha.value
            rotationZ = rotation.value
            scaleX = .85f + alpha.value * .15f
            scaleY = scaleX
        }.clearAndSetSemantics {})
}
