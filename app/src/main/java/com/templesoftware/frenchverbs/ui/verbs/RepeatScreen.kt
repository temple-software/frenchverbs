package com.templesoftware.frenchverbs.ui.verbs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.templesoftware.frenchverbs.R
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.repeat.RepeatTimeline
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.*

private val RepeatThirdRowCenter = HorizontalAlignmentLine { a,b -> minOf(a,b) }

/** One frame-driven clock, active only while this destination is RESUMED.
 * Each lifecycle restart resets its frame origin, so background time is never consumed. */
@Composable
fun RepeatClock(model: RepeatViewModel) {
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val state by model.state.collectAsState()
    LaunchedEffect(model,lifecycle,state.playback,state.complete) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var previous: Long? = null
            var remainder=0L
            while(!model.state.value.complete && model.state.value.playback!=RepeatPlayback.WAITING && model.state.value.current!=null) {
                withFrameNanos { now ->
                    previous?.let { before ->
                        // A stalled rendering frame must not skip a teaching phase.
                        val delta=(now-before).coerceIn(0,100_000_000)+remainder
                        model.tick(delta/1_000_000);remainder=delta%1_000_000
                    }
                    previous=now
                }
            }
        }
    }
}

@Composable
fun RepeatScreen(state: RepeatState, timeline: RepeatTimeline, language: AppLanguage,
    onHome: () -> Unit, onTry: () -> Unit, onNext: () -> Unit = {}) {
    val item=state.current ?: return
    val verb=remember(item.verbId){OfflineDictionary.verbs.first{it.id==item.verbId}}
    val english=language==AppLanguage.ENGLISH_UK
    val config=LocalConfiguration.current
    val landscape=config.screenWidthDp>config.screenHeightDp
    
    val tablet=minOf(config.screenWidthDp,config.screenHeightDp)>=600
    val alpha=if(state.complete) { if(state.single)0f else 1f } else timeline.contentAlpha(state.elapsed)
    val instruction=(if(english)"Repeat out loud" else "Répète à voix haute")+" ... ${timeline.countdown(state.elapsed)}"
    val frenchTense=stringResource(ChoiceResources.french.getValue(Choices.tenses[item.tense]))
    val verbLabel=verb.id+if(english)" / ${verb.meaning}" else ""
    Scaffold(contentWindowInsets=WindowInsets.safeDrawing,topBar={
        Surface(color=MaterialTheme.colorScheme.surfaceContainer) {
            Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top+WindowInsetsSides.Horizontal))
                .heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onHome,modifier=Modifier.testTag("repeat_home")) {
                    Icon(Icons.Default.Home,stringResource(R.string.navigate_home),tint=MaterialTheme.colorScheme.primary)
                }
                if(landscape) {
                    BoxWithConstraints(Modifier.weight(1f)) {
                        val primary=MaterialTheme.colorScheme.primary
                        val title=buildAnnotatedString {
                            append(if(english)"Repeat: " else "Répéter : ")
                            withStyle(SpanStyle(color=primary.copy(alpha=alpha*timeline.headingVerbAlpha(state.elapsed)))){append(verb.id)}
                            append(" - ")
                            withStyle(SpanStyle(color=primary.copy(alpha=alpha*timeline.headingTenseAlpha(state.elapsed)))){append(frenchTense.replaceFirstChar{it.lowercase()})}
                        }
                        val measurer=rememberTextMeasurer()
                        val density=androidx.compose.ui.platform.LocalDensity.current
                        val base=MaterialTheme.typography.titleLarge
                        val titleSize=remember(title.text,maxWidth,density,base) {
                            val width=with(density){maxWidth.roundToPx()}
                            (22 downTo 16).firstOrNull{measurer.measure(title.text,base.copy(fontSize=it.sp),softWrap=false).size.width<=width} ?: 16
                        }
                        Text(title,Modifier.fillMaxWidth().testTag("repeat_title"),style=base.copy(fontSize=titleSize.sp,lineHeight=(titleSize+4).sp),
                            color=primary,textAlign=TextAlign.Center,maxLines=2)
                    }
                } else {
                Text(if(english)"Repeat" else "Répéter",Modifier.weight(1f).testTag("repeat_title"),
                    style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary,textAlign=TextAlign.Center)
                }
                Spacer(Modifier.width(48.dp))
            }
        }
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets),horizontalAlignment=Alignment.CenterHorizontally) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().testTag("repeat_viewport")) {
                val density=androidx.compose.ui.platform.LocalDensity.current
                val compact=landscape && maxHeight<240.dp
                val scroll=rememberScrollState()
                val contentWidth=minOf(maxWidth-32.dp,800.dp)
                val viewportHeight=maxHeight
                val topSpace=if(compact)8.dp else minOf(maxHeight*.04f,24.dp)+if(landscape)4.dp else 0.dp
                val french=stringResource(ChoiceResources.french.getValue(Choices.tenses[item.tense]))
                val name=french+if(english)" (${stringResource(ChoiceResources.tense.getValue(Choices.tenses[item.tense]))})" else ""
                val headingStyle=MaterialTheme.typography.titleMedium.copy(
                    fontSize=if(compact)18.sp else if(landscape)23.sp else 22.sp,
                    lineHeight=if(compact)22.sp else 28.sp,fontWeight=FontWeight.Bold)
                val verbStyle=headingStyle.copy(fontSize=if(compact)24.sp else 30.sp,lineHeight=if(compact)28.sp else 36.sp)
                val instructionStyle=MaterialTheme.typography.titleMedium.copy(
                    fontSize=if(compact)16.sp else 22.sp,lineHeight=if(compact)19.sp else 28.sp)
                val instructionAlpha=timeline.instructionAlpha(state.elapsed)
                val ink=MaterialTheme.colorScheme.onSurface
                val green=learnGreen()
                val forms=verb.tenses[item.tense].forms
                val measurer=rememberTextMeasurer()
                val texts=remember(forms){forms.map{form->buildAnnotatedString {
                    append(form.subject+form.before)
                    withStyle(SpanStyle(fontWeight=FontWeight.Bold)){append(form.marked)}
                    append(form.after)
                }}}
                val baseStyle=MaterialTheme.typography.bodyLarge
                val style=remember(texts,baseStyle,headingStyle,verbStyle,instructionStyle,name,verbLabel,density,contentWidth,viewportHeight,landscape,compact,tablet) {
                    fun candidate(size:Int)=baseStyle.copy(fontSize=size.sp,
                        lineHeight=if(landscape)(size+4).sp else if(compact)(size+2).sp else if(tablet)(size+10).sp else (size+9).sp,
                        platformStyle=PlatformTextStyle(includeFontPadding=false))
                    if(landscape) {
                        val width=with(density){contentWidth.roundToPx()}
                        val available=with(density){(viewportHeight-16.dp).roundToPx()}
                        val desired=if(tablet)48 else 32
                        return@remember candidate((desired downTo (if(compact)22 else 24)).firstOrNull { size ->
                            texts.sumOf{measurer.measure(it,candidate(size),constraints=Constraints(maxWidth=width)).size.height}<=available
                        } ?: (if(compact)22 else 24))
                    }
                    val desired=if(compact)21 else if(tablet) {if(landscape)32 else 30} else if(landscape)27 else 25
                    val width=with(density){contentWidth.roundToPx()}
                    val headingHeight=measurer.measure(name,headingStyle,constraints=Constraints(maxWidth=width)).size.height
                    val verbHeight=measurer.measure(verbLabel,verbStyle,constraints=Constraints(maxWidth=width)).size.height
                    val promptHeight=measurer.measure(instruction,instructionStyle,constraints=Constraints(maxWidth=width)).size.height
                    val available=with(density){viewportHeight.roundToPx()-topSpace.roundToPx()-16.dp.roundToPx()}-headingHeight-verbHeight-promptHeight
                    val sizes=if(compact && density.fontScale<=1f)listOf(21,20,19) else listOf(desired)
                    sizes.firstOrNull { size ->
                        val height=texts.sumOf{measurer.measure(it,candidate(size),constraints=Constraints(maxWidth=width)).size.height}
                        height<=available
                    }?.let(::candidate) ?: candidate(sizes.last())
                }
                val blockWidth=remember(texts,style,density,contentWidth) {
                    val widest=texts.maxOf{measurer.measure(it,style,softWrap=false).size.width}
                    minOf(contentWidth,with(density){widest.toDp()}+2.dp)
                }
                Column(Modifier.fillMaxSize().verticalScroll(scroll).testTag("repeat_scroll"),horizontalAlignment=Alignment.CenterHorizontally) {
                    Layout(modifier=Modifier.width(contentWidth).heightIn(min=viewportHeight).alpha(alpha),content={
                        if(landscape)Spacer(Modifier.size(0.dp)) else Text(verbLabel,Modifier.fillMaxWidth().alpha(timeline.headingVerbAlpha(state.elapsed)).testTag("repeat_verb"),
                            style=verbStyle,color=green,textAlign=TextAlign.Center)
                        if(landscape)Spacer(Modifier.size(0.dp)) else Text(name,Modifier.fillMaxWidth().alpha(timeline.headingTenseAlpha(state.elapsed)).testTag("repeat_tense"),style=headingStyle,
                            color=MaterialTheme.colorScheme.primary,textAlign=TextAlign.Center)
                        Text(instruction,Modifier.alpha(instructionAlpha).testTag("repeat_instruction")
                            .clearAndSetSemantics { if(instructionAlpha>0f)text=AnnotatedString(instruction) },
                            style=instructionStyle,textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.primary)
                        Column(Modifier.width(blockWidth).then(if(landscape)Modifier.heightIn(min=(viewportHeight-16.dp).coerceAtLeast(0.dp)) else Modifier).testTag("repeat_rows"),
                            verticalArrangement=if(landscape)Arrangement.SpaceEvenly else Arrangement.spacedBy(if(compact)0.dp else 8.dp)) {
                            forms.forEachIndexed { row,form ->
                                val subject=timeline.subjectAlpha(row,state.elapsed)
                                val rest=timeline.verbAlpha(row,state.elapsed)
                                val text=buildAnnotatedString {
                                    withStyle(SpanStyle(color=ink.copy(alpha=subject))){append(form.subject)}
                                    withStyle(SpanStyle(color=ink.copy(alpha=rest))){append(form.before)}
                                    withStyle(SpanStyle(color=green.copy(alpha=rest),fontWeight=FontWeight.Bold)){append(form.marked)}
                                    withStyle(SpanStyle(color=ink.copy(alpha=rest))){append(form.after)}
                                }
                                val spoken=if(rest>0f)form.text else if(subject>0f)form.subject.trimEnd() else ""
                                val rendered=remember{arrayOfNulls<TextLayoutResult>(1)}
                                Text(text,Modifier.fillMaxWidth().then(if(row==2) Modifier.layout { measurable, constraints ->
                                    val measured=measurable.measure(constraints)
                                    layout(measured.width,measured.height,mapOf(RepeatThirdRowCenter to measured.height/2)) {
                                        measured.placeRelative(0,0)
                                    }
                                } else Modifier).testTag("repeat_row_$row").clearAndSetSemantics {
                                    if(spoken.isNotEmpty())this.text=AnnotatedString(spoken)
                                    getTextLayoutResult { results -> rendered[0]?.let{results.add(it);true} ?: false }
                                },style=style,onTextLayout={rendered[0]=it})
                            }
                        }
                    }) { children,constraints ->
                        val childBounds=constraints.copy(minWidth=0,minHeight=0)
                        val verbHeading=children[0].measure(childBounds)
                        val heading=children[1].measure(childBounds)
                        val prompt=children[2].measure(childBounds)
                        val group=children[3].measure(childBounds)
                        val top=topSpace.roundToPx()
                        val gap=(if(landscape)4.dp else 12.dp).roundToPx()
                        val bottom=(if(landscape)8.dp else topSpace).roundToPx()
                        val tenseY=top+verbHeading.height+4.dp.roundToPx()
                        val promptY=tenseY+heading.height+gap
                        val regionTop=if(landscape)8.dp.roundToPx() else promptY+prompt.height+gap
                        val portraitStart=regionTop+18.dp.roundToPx()
                        val minimum=if(landscape)regionTop+group.height+bottom else portraitStart+group.height+bottom
                        val height=maxOf(constraints.minHeight,minimum)
                        val groupY=if(landscape)regionTop+(height-bottom-regionTop-group.height)/2 else portraitStart
                        layout(constraints.maxWidth,height) {
                            verbHeading.placeRelative((constraints.maxWidth-verbHeading.width)/2,top)
                            heading.placeRelative((constraints.maxWidth-heading.width)/2,tenseY)
                            // Align with the actual third row; it is still hidden while the countdown fades.
                            prompt.placeRelative((constraints.maxWidth-prompt.width)/2,groupY+group[RepeatThirdRowCenter]-prompt.height/2)
                            group.placeRelative((constraints.maxWidth-group.width)/2,groupY)
                        }
                    }
                }
            }
            // Allocate the bottom action for every phase, including Single Repeat.
            RepeatNextButton(state,english,onNext,onTry,Modifier.padding(horizontal=16.dp,vertical=4.dp))
        }
    }
}



