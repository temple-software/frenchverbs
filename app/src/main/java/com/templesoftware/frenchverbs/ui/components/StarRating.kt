package com.templesoftware.frenchverbs.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.templesoftware.frenchverbs.R
import com.templesoftware.frenchverbs.ui.theme.educationalColors

@Composable
fun StarRating(
    stars: Int,
    starSize: Dp,
    modifier: Modifier = Modifier,
    richSurface: Boolean = false,
) {
    require(stars in 0..5)
    val educationalColors = MaterialTheme.educationalColors
    val description = pluralStringResource(R.plurals.results_star_rating_description, stars, stars, 5)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) { index ->
            val awarded = index < stars
            Icon(
                imageVector = if (awarded) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (awarded) educationalColors.goldStar
                else if (richSurface) educationalColors.unearnedStarOnRichSurface
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                modifier = Modifier.size(starSize),
            )
        }
    }
}

