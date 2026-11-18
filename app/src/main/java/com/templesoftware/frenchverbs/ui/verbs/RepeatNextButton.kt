package com.templesoftware.frenchverbs.ui.verbs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.domain.verbs.*

@Composable
internal fun RepeatNextButton(state:RepeatState,english:Boolean,onNext:()->Unit,onTry:()->Unit,modifier:Modifier=Modifier) {
    val visible=!state.single && !state.complete && state.playback!=RepeatPlayback.PLAYING
    val enabled=visible && state.playback==RepeatPlayback.WAITING
    val next=state.next
    val green=learnGreen();val primary=MaterialTheme.colorScheme.primary
    val style=MaterialTheme.typography.labelLarge
    val measurer=rememberTextMeasurer();val density=LocalDensity.current
    val tense=if(next==null)"" else if(english)stringResource(ChoiceResources.tense.getValue(Choices.tenses[next.tense])).lowercase(java.util.Locale.UK)
        else listOf("présent","imparfait","futur simple","passé composé","passé simple","plus-que-parfait")[next.tense]
    fun label(prefix:Boolean,split:Boolean=false)=buildAnnotatedString {
        if(next==null)append(if(english)"Try yourself" else "À ton tour !") else {
            if(prefix)append(if(english)"Next: " else "Suivant : ")
            withStyle(SpanStyle(color=green,fontWeight=FontWeight.Bold)){append(next.verbId)}
            append(if(split)"\n" else " ")
            append(if(english)"in " else if(next.tense==1)"à l’" else "au ")
            withStyle(SpanStyle(color=primary,fontWeight=FontWeight.Bold)){append(tense)}
        }
    }
    BoxWithConstraints(modifier.widthIn(max=900.dp).fillMaxWidth().testTag("repeat_navigation")) {
        val available=with(density){maxWidth.roundToPx()-2*12.dp.roundToPx()-24.dp.roundToPx()-8.dp.roundToPx()}.coerceAtLeast(1)
        val prefix=measurer.measure(label(true),style,softWrap=false).size.width<=available
        val text=if(prefix)label(true) else label(false,measurer.measure(label(false),style,softWrap=false).size.width>available)
        val measured=measurer.measure(text,style,constraints=Constraints(maxWidth=available))
        val height=maxOf(48.dp,with(density){(kotlin.math.ceil(measured.multiParagraph.height).toInt()+2*6.dp.roundToPx()).toDp()})
        OutlinedButton(onClick={if(next==null)onTry() else onNext()},enabled=enabled,
            modifier=Modifier.fillMaxWidth().height(height).alpha(if(visible)1f else 0f)
                .then(if(visible)Modifier.testTag(if(next==null)"repeat_try" else "repeat_next") else Modifier.clearAndSetSemantics{}),
            shape=RoundedCornerShape(18.dp),border=BorderStroke(2.dp,primary),contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp)) {
            Text(text,Modifier.weight(1f).testTag("repeat_next_label"),style=style,textAlign=TextAlign.Center)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward,null,Modifier.size(24.dp).testTag("repeat_next_arrow"))
        }
    }
}
