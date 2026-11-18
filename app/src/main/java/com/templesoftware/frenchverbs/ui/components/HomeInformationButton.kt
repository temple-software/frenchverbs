package com.templesoftware.frenchverbs.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.templesoftware.frenchverbs.R

@Composable
fun HomeInformationButton(onAbout: () -> Unit, modifier: Modifier = Modifier) {
    val aboutDescription = stringResource(R.string.about_open, stringResource(R.string.app_name))
    IconButton(onClick = onAbout, modifier = modifier.size(48.dp).testTag("open_about")
        .semantics { contentDescription = aboutDescription }) {
        Box(Modifier.size(32.dp).background(Color(0xFFE7F1FD), CircleShape)
            .border(1.dp, Color(0xFFBCCFE8), CircleShape), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(12.dp, 20.dp)) {
                val ink = Color(0xFF234A7C)
                drawCircle(ink, 1.6.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width / 2, 3.dp.toPx()))
                drawLine(ink, androidx.compose.ui.geometry.Offset(size.width / 2, 8.dp.toPx()),
                    androidx.compose.ui.geometry.Offset(size.width / 2, 17.dp.toPx()),
                    2.5.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
    }
}
