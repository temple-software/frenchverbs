package com.templesoftware.frenchverbs.ui.verbs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.templesoftware.frenchverbs.R
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.VerbState

/** Width measurement includes styled glyphs, font scaling, padding and the reserved arrow. */
@Composable
internal fun LearnNavigationBar(state: VerbState, english: Boolean, compact: Boolean,
    onPrevious: () -> Unit, onNext: () -> Unit, onRepeat: (() -> Unit)? = null) {
    val style=MaterialTheme.typography.labelLarge
    val measurer=rememberTextMeasurer()
    val density=LocalDensity.current
    val repeat=stringResource(R.string.screen_repeat)
    val ending=stringResource(R.string.now_repeat).removeSuffix(" →")
    val next=state.next
    val tense=if(next==null) "" else if(english)
        stringResource(ChoiceResources.tense.getValue(Choices.tenses[next.tense])).lowercase(java.util.Locale.UK)
        else listOf("présent","imparfait","futur simple","passé composé","passé simple","plus-que-parfait")[next.tense]
    val green=learnGreen()
    val primary=MaterialTheme.colorScheme.primary
    fun label(prefix:Boolean, split:Boolean=false)=buildAnnotatedString {
        if(next==null) append(ending) else {
            if(prefix)append(if(english) "Next: " else "Suivant : ")
            withStyle(SpanStyle(color=green,fontWeight=FontWeight.Bold)){append(next.verbId)}
            append(if(split) "\n" else " ")
            append(if(english) "in " else if(next.tense==1) "à l’" else "au ")
            withStyle(SpanStyle(color=primary,fontWeight=FontWeight.Bold)){append(tense)}
        }
    }
    BoxWithConstraints(Modifier.widthIn(max=1100.dp).fillMaxWidth()
        .padding(vertical=if(compact)2.dp else 6.dp).testTag("learn_navigation")) {
        val gap=if(maxWidth<360.dp)6.dp else 8.dp
        val backWidth=if(compact || maxWidth<360.dp)48.dp else 54.dp
        val repeatWidth=with(density){measurer.measure(repeat,style,softWrap=false).size.width.toDp()}+20.dp
        // Match the separately rounded pixel sizes used by Row and button padding.
        val labelWidth=with(density){maxWidth.roundToPx()-backWidth.roundToPx()-repeatWidth.roundToPx()-
            2*gap.roundToPx()-2*8.dp.roundToPx()-24.dp.roundToPx()-6.dp.roundToPx()}.coerceAtLeast(1)
        val full=label(true)
        val hasPrefix=measurer.measure(full,style,softWrap=false).size.width<=labelWidth
        val short=label(false)
        val text=if(hasPrefix)full else label(false,measurer.measure(short,style,softWrap=false).size.width>labelWidth)
        val measured=measurer.measure(text,style,constraints=Constraints(maxWidth=labelWidth))
        val height=maxOf(if(compact)48.dp else 54.dp,with(density){
            (kotlin.math.ceil(measured.multiParagraph.height).toInt()+2*6.dp.roundToPx()).toDp()
        })
        val shape=RoundedCornerShape(18.dp)
        Row(Modifier.fillMaxWidth().height(height),horizontalArrangement=Arrangement.spacedBy(gap),verticalAlignment=Alignment.CenterVertically) {
            OutlinedButton(onClick=onPrevious,enabled=state.index>0,
                modifier=Modifier.width(backWidth).fillMaxHeight().testTag("learn_previous"),
                shape=shape,contentPadding=PaddingValues(0.dp),
                border=BorderStroke(2.dp,primary.copy(alpha=if(state.index>0)1f else .3f))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack,stringResource(R.string.navigate_back),Modifier.size(28.dp))
            }
            Button(onClick={onRepeat?.invoke()},enabled=onRepeat!=null,
                modifier=Modifier.width(repeatWidth).fillMaxHeight().testTag("learn_repeat"),
                shape=shape,contentPadding=PaddingValues(horizontal=10.dp),
                colors=ButtonDefaults.buttonColors(disabledContainerColor=Color(0xFFE0E0E0),disabledContentColor=Color(0xFF777777))) {
                Text(repeat,style=style,maxLines=1)
            }
            OutlinedButton(onClick=onNext,modifier=Modifier.weight(1f).fillMaxHeight().testTag("learn_next"),
                shape=shape,border=BorderStroke(2.dp,primary),contentPadding=PaddingValues(horizontal=8.dp,vertical=6.dp)) {
                Text(text,Modifier.weight(1f).testTag("learn_next_label"),style=style,textAlign=TextAlign.Center)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward,null,Modifier.size(24.dp).testTag("learn_next_arrow"))
            }
        }
    }
}
