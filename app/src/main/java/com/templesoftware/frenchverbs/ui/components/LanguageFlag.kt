package com.templesoftware.frenchverbs.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.templesoftware.frenchverbs.data.preferences.AppLanguage

@Composable
fun LanguageFlag(language: AppLanguage, modifier: Modifier = Modifier) {
    Image(painterResource(language.flagResource), null,
        modifier.width(28.dp).aspectRatio(3f / 2f).clip(RoundedCornerShape(2.dp)))
}
