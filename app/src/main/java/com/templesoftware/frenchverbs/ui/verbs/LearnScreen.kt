package com.templesoftware.frenchverbs.ui.verbs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.templesoftware.frenchverbs.R
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.VerbState

/** Learn has its own header so approved selection and information screens stay unchanged. */
@Composable
fun LearnScreen(state: VerbState, language: AppLanguage, onHome: () -> Unit,
    onNext: () -> Unit, onPrevious: () -> Unit = {}, onRepeat: (() -> Unit)? = null) {
    val item=state.current ?: return
    val verb=OfflineDictionary.verbs.first { it.id==item.verbId }
    val exampleState=rememberSaveable(saver=LearnExampleState.saver){LearnExampleState()}
    val english=language==AppLanguage.ENGLISH_UK
    val comparison=when(item.tense){1->0;4->1;5->3;else->item.tense}
    val landscape=LocalConfiguration.current.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val shortLandscape=landscape && LocalConfiguration.current.screenHeightDp<=420
    Scaffold(contentWindowInsets=WindowInsets.safeDrawing,topBar={
        Surface(color=MaterialTheme.colorScheme.surfaceContainer) {
            Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top+WindowInsetsSides.Horizontal))
                .heightIn(min=if(landscape)48.dp else 64.dp).padding(end=12.dp).testTag("learn_bar"),
                verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onHome,modifier=Modifier.testTag("learn_home")) {
                    Icon(Icons.Default.Home,stringResource(R.string.navigate_home),tint=MaterialTheme.colorScheme.primary)
                }
                Text(verb.id+if(english) " / ${verb.meaning}" else "",
                    Modifier.weight(1f).padding(horizontal=8.dp,vertical=4.dp).testTag("screen_title"),
                    style=MaterialTheme.typography.titleLarge,textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.primary)
                Text("${learnProgress(state.index,state.session.size)}%",Modifier.testTag("learn_progress"),
                    style=MaterialTheme.typography.labelMedium.copy(fontSize=14.sp,lineHeight=18.sp),
                    color=MaterialTheme.colorScheme.primary)
            }
        }
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).padding(horizontal=12.dp),
            horizontalAlignment=Alignment.CenterHorizontally) {
            val scroll=rememberScrollState()
            LaunchedEffect(state.index) { scroll.scrollTo(0) }
            BoxWithConstraints(Modifier.weight(1f).widthIn(max=1100.dp).fillMaxWidth().testTag("learn_viewport")) {
                val paired=comparison!=item.tense
                val denseLandscape=landscape && (maxHeight<210.dp || (maxHeight<280.dp && LocalDensity.current.fontScale in 1.01f..1.5f))
                val tightPortrait=LocalConfiguration.current.screenHeightDp<850 || LocalDensity.current.fontScale>1.3f
                val insetTight=insets.calculateBottomPadding()>30.dp
                val crampedPortrait=!landscape && ((paired && insetTight && maxHeight<520.dp) || (LocalDensity.current.fontScale in 1.01f..1.5f && maxHeight<560.dp))
                val outer=if(crampedPortrait)0.dp else if(!landscape && insetTight && maxHeight<560.dp)4.dp else if(denseLandscape)0.dp else if(landscape)4.dp else if(tightPortrait)8.dp else 12.dp
                val bottomOuter=if(landscape) (if(denseLandscape)3.dp else if(shortLandscape)12.dp else 10.dp) else outer
                val panelWidth=if(landscape)(maxWidth-10.dp)/2 else maxWidth
                val examples=if(paired)LearnExamples.forPanel(verb,item.tense,true,english) else emptyList()
                val otherExamples=if(paired)LearnExamples.forPanel(verb,comparison,true,english) else emptyList()
                val measurer=rememberTextMeasurer()
                val density=LocalDensity.current
                val green=learnGreen()
                val horizontal=if(panelWidth<360.dp)18.dp else if(panelWidth>=600.dp)24.dp else 20.dp
                val exampleStyle=learnExampleStyle(landscape,shortLandscape,tightPortrait)
                // Matching sentence rows share the taller measured height, including wrapping.
                val exampleHeights=if(landscape && paired) examples.indices.map { i ->
                    val width=with(density){(panelWidth-horizontal*2).roundToPx()}.coerceAtLeast(1)
                    val height=maxOf(measurer.measure(exampleText(examples[i],green),exampleStyle,constraints=Constraints(maxWidth=width)).size.height,
                        measurer.measure(exampleText(otherExamples[i],green),exampleStyle,constraints=Constraints(maxWidth=width)).size.height)
                    with(density){height.toDp()}
                } else emptyList()
                val sentenceStyle=randomSentenceStyle(landscape,shortLandscape)
                val config=LocalConfiguration.current
                // Fit the actual width, plus the narrower rotated presentation, before choosing.
                val rotatedWidth=if(landscape)minOf(config.screenHeightDp-24,1100).dp else ((minOf(config.screenHeightDp-72,1100)-10)/2).dp
                val fitWidth=with(density){(minOf(panelWidth,rotatedWidth)-horizontal*2).toPx()}
                val portraitStyle=randomSentenceStyle(false,false).copy(fontSize=if(maxOf(config.screenWidthDp,config.screenHeightDp)<850 || density.fontScale>1.3f)19.sp else 20.sp)
                val landscapeStyle=randomSentenceStyle(true,minOf(config.screenWidthDp,config.screenHeightDp)<=420)
                // Selection repeatedly visits the same candidates. Retain their stable widths
                // for this item and typography; theme, density and rotation invalidate the cache.
                val widths=remember(verb.id,item.tense,portraitStyle,landscapeStyle,sentenceStyle,green,density) {
                    mutableMapOf<SentenceBank.Sentence,Float>()
                }
                val measurementStyles=listOf(portraitStyle,landscapeStyle,sentenceStyle).distinct()
                val widthOf:(SentenceBank.Sentence)->Float={sentence ->
                    widths.getOrPut(sentence) {
                        val text=sentenceText(sentence,green)
                        measurementStyles.maxOf{measurer.measure(text,it,softWrap=false).size.width}.toFloat()
                    }
                }
                val itemKey="${state.session.hashCode()}:${state.index}:${item.encode()}"
                LaunchedEffect(itemKey) { exampleState.enter(itemKey,verb,item.tense,widthOf,fitWidth) }
                val sentences=if(exampleState.itemKey==itemKey)exampleState.sentences(verb,item.tense) else emptyList()
                val toggle={exampleState.toggle(verb,item.tense,widthOf,fitWidth)}
                val contentModifier=Modifier.fillMaxWidth().verticalScroll(scroll).padding(top=outer,bottom=bottomOuter).testTag("learn_panels")
                @Composable fun PortraitPanels() {
                    Box(Modifier.fillMaxWidth()) {
                        LearnPanel(verb,item.tense,english,paired,paired,true,paired,
                            false,shortLandscape,tightPortrait,crampedPortrait,panelWidth,horizontal,exampleHeights,Modifier.fillMaxWidth(),"current")
                        ExamplesToggle(exampleState.active,false,english,toggle,Modifier.align(Alignment.BottomEnd).padding(end=8.dp,bottom=4.dp))
                    }
                    if(exampleState.active)RandomExamplesPanel(sentences,Modifier.fillMaxWidth(),horizontal)
                    else LearnPanel(verb,comparison,english,true,true,paired,paired,
                        false,shortLandscape,tightPortrait,crampedPortrait,panelWidth,horizontal,exampleHeights,Modifier.fillMaxWidth(),"comparison")
                }
                // Match padding's pixel rounding before converting the remaining height to dp.
                val panelMinHeight=with(density) {
                    (maxHeight.roundToPx()-outer.roundToPx()-bottomOuter.roundToPx()).coerceAtLeast(0).toDp()
                }
                if(landscape) LearnLandscapePanels(verb,item.tense,comparison,english,shortLandscape,denseLandscape,
                    panelWidth,horizontal,contentModifier.heightIn(min=panelMinHeight),exampleState.active,sentences,toggle)
                else Column(contentModifier,verticalArrangement=Arrangement.spacedBy(if(crampedPortrait)2.dp else if(tightPortrait)12.dp else 16.dp)) { PortraitPanels() }
            }
            LearnNavigationBar(state, english, shortLandscape, onPrevious, onNext, onRepeat)
        }
    }
}

@Composable internal fun learnGreen()=if(MaterialTheme.colorScheme.background.luminance()<.5f) Color(0xFF87E3AD) else Color(0xFF17683D)

@Composable
internal fun LearnTenseHeading(tense:Int,english:Boolean,tag:String,compact:Boolean,width:Dp,horizontal:Dp) {
    val french=stringResource(ChoiceResources.french.getValue(Choices.tenses[tense]))
    val translation=if(english)stringResource(ChoiceResources.tense.getValue(Choices.tenses[tense])) else ""
    val density=LocalDensity.current
    val measurer=rememberTextMeasurer()
    run {
        fun heading(size:Float)=buildAnnotatedString {
            withStyle(SpanStyle(fontWeight=FontWeight.Bold)){append(french)}
            if(english) withStyle(SpanStyle(fontSize=(size*.85f).sp,fontWeight=FontWeight.Normal)){append(" ($translation)")}
        }
        val base=MaterialTheme.typography.titleMedium.copy(fontSize=18.sp,lineHeight=22.sp,platformStyle=PlatformTextStyle(includeFontPadding=false))
        var size=if(compact)16f else 18f
        // Keep normal-size bilingual headings inline; accessibility may wrap complete words.
        if(density.fontScale<=1.3f) while(size>14f && measurer.measure(heading(size),base.copy(fontSize=size.sp),softWrap=false).size.width>with(density){(width-horizontal*2).toPx()}) size-=.5f
        Text(heading(size),Modifier.testTag("learn_heading_$tag"),style=base.copy(fontSize=size.sp,lineHeight=(size+if(compact)1 else 3).sp),color=MaterialTheme.colorScheme.primary)
    }
}

internal fun learnPanelColor(scheme: ColorScheme, comparison: Boolean): Color =
    if(!comparison) scheme.surfaceContainerLow else if(scheme.background.luminance()<.5f)
        lerp(scheme.surface,scheme.primaryContainer,.2f) else lerp(scheme.surface,scheme.surfaceContainer,.6f)

internal fun exampleText(sentence:LearnExample,green:Color)=buildAnnotatedString {
    append(sentence.before)
    if(sentence.marked.isNotEmpty())withStyle(SpanStyle(color=green,fontWeight=FontWeight.Bold)){append(sentence.marked)}
    append(sentence.after)
}

@Composable internal fun learnExampleStyle(landscape:Boolean,compact:Boolean,tightPortrait:Boolean)=
    MaterialTheme.typography.bodySmall.copy(fontSize=if(landscape) (if(compact)13.sp else 15.sp) else if(tightPortrait)16.sp else 17.sp,
        lineHeight=if(compact)15.sp else if(landscape)19.sp else if(tightPortrait)20.sp else 22.sp,
        platformStyle=PlatformTextStyle(includeFontPadding=false),
        lineHeightStyle=androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,androidx.compose.ui.text.style.LineHeightStyle.Trim.Both))

@Composable
private fun LearnPanel(verb:Verb,tense:Int,english:Boolean,highlight:Boolean,showExamples:Boolean,
    heading:Boolean,paired:Boolean,landscape:Boolean,compact:Boolean,tightPortrait:Boolean,denseLandscape:Boolean,
    panelWidth:Dp,horizontal:Dp,exampleHeights:List<Dp>,modifier:Modifier,tag:String) {
    val data=verb.tenses[tense]
    val green=learnGreen()
    val bottomExamples=paired && (landscape || tag=="current")
    Surface(modifier.testTag("learn_panel_$tag"),shape=RoundedCornerShape(18.dp),
        color=learnPanelColor(MaterialTheme.colorScheme,tag=="comparison"),
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Column((if(landscape)Modifier.fillMaxHeight() else Modifier).padding(horizontal=horizontal,
            vertical=if(denseLandscape && !landscape && LocalDensity.current.fontScale in 1.01f..1.5f)2.dp else if(denseLandscape)4.dp else if(compact)6.dp else if(landscape)10.dp else if(tightPortrait)10.dp else 12.dp).padding(end=if(tag=="current")48.dp else 0.dp)) {
            if(heading) {
                LearnTenseHeading(tense,english,tag,compact,panelWidth,horizontal)
                Spacer(Modifier.height(if(denseLandscape)2.dp else if(compact)4.dp else 8.dp))
            }
            @Composable fun Examples() {
                LearnExamples.forPanel(verb,tense,paired,english).forEachIndexed { index,sentence ->
                    Text(exampleText(sentence,green),Modifier.fillMaxWidth()
                        .heightIn(min=exampleHeights.getOrElse(index){0.dp}).testTag("learn_example_${tag}_$index"),
                        style=learnExampleStyle(landscape,compact,tightPortrait))
                    if(index==0)Spacer(Modifier.height(if(denseLandscape)1.dp else if(compact)2.dp else 4.dp))
                }
            }
            @Composable fun Divider() {
                HorizontalDivider(Modifier.padding(vertical=if(denseLandscape)2.dp else if(compact)3.dp else 6.dp).testTag("learn_divider_$tag"),thickness=.5.dp)
            }
            if(showExamples && !bottomExamples) { Examples(); Divider() }
            fun formText(form:Form)=buildAnnotatedString {
                append(form.subject);append(form.before)
                if(highlight) withStyle(SpanStyle(color=green,fontWeight=FontWeight.Bold)){append(form.marked)} else append(form.marked)
                append(form.after)
            }
            if(denseLandscape && landscape) {
                // On the shortest viewport a single paragraph avoids repeating each row's
                // font-metric leading. Keep the same 17sp glyphs and all six lines.
                Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.CenterStart) {
                    Text(buildAnnotatedString {
                        data.forms.forEachIndexed { index,form ->
                            if(index>0)append("\n")
                            append(formText(form))
                        }
                    },Modifier.fillMaxWidth().testTag("learn_forms_$tag"),style=MaterialTheme.typography.bodyLarge.copy(
                        fontSize=17.sp,lineHeight=17.sp,platformStyle=PlatformTextStyle(includeFontPadding=false),
                        lineHeightStyle=androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,androidx.compose.ui.text.style.LineHeightStyle.Trim.Both)))
                }
            } else if(landscape) {
                // The conjugation group, not an empty spacer below it, owns spare height.
                Column(Modifier.weight(1f).fillMaxWidth().testTag("learn_forms_$tag"),
                    verticalArrangement=Arrangement.SpaceBetween) {
                    data.forms.forEachIndexed { index,form ->
                        Text(formText(form),Modifier.fillMaxWidth().testTag("learn_form_${tag}_$index"),
                            style=MaterialTheme.typography.bodyLarge.copy(fontSize=if(compact)17.sp else 18.sp,
                                lineHeight=if(compact)18.sp else 22.sp,platformStyle=PlatformTextStyle(includeFontPadding=false),
        lineHeightStyle=androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,androidx.compose.ui.text.style.LineHeightStyle.Trim.Both)))
                    }
                }
            } else {
                Text(buildAnnotatedString {
                    data.forms.forEachIndexed { index,form ->
                        if(index>0)append("\n")
                        append(formText(form))
                    }
                },Modifier.fillMaxWidth().testTag("learn_forms_$tag"),
                    style=if(denseLandscape)LocalTextStyle.current.copy(platformStyle=PlatformTextStyle(includeFontPadding=false),lineHeightStyle=androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,androidx.compose.ui.text.style.LineHeightStyle.Trim.Both))else LocalTextStyle.current,
                    fontSize=if(tightPortrait)19.sp else 20.sp,lineHeight=if(denseLandscape && !landscape && LocalDensity.current.fontScale in 1.01f..1.5f)21.sp else if(tightPortrait)24.sp else 26.sp)
            }
            if(highlight && verb.auxiliary=="être" && tense in listOf(3,5)) {
                Text(stringResource(R.string.agreement),Modifier.padding(top=2.dp),style=if(compact)MaterialTheme.typography.labelSmall.copy(fontSize=11.sp,lineHeight=12.sp) else MaterialTheme.typography.labelSmall)
            }
            if(showExamples && bottomExamples) {
                Divider(); Examples()
            }
        }
    }
}

/** Endpoints are exclusive; half-way values round upward to the next five percent. */
internal fun learnProgress(index: Int, total: Int): Int = when {
    total <= 1 -> 100
    index <= 0 -> 0
    index >= total - 1 -> 100
    else -> (kotlin.math.floor(index.toDouble() / (total - 1) * 20 + .5).toInt() * 5).coerceIn(5,95)
}
