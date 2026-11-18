package com.templesoftware.frenchverbs.ui.quiz

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.quiz.*
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.ConjugateViewModel
import com.templesoftware.frenchverbs.ui.components.PremiumGradientButton
import com.templesoftware.frenchverbs.ui.theme.*
import com.templesoftware.frenchverbs.ui.verbs.ChoiceResources

/** One lifecycle-bound clock only while red feedback is active; rotation retains remaining time. */
@Composable fun QuizFeedbackClock(model:ConjugateViewModel) {
    val state by model.state.collectAsState()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(model,lifecycle,state.running,state.feedbackRemaining>0) {
        if(state.running && state.feedbackRemaining>0)lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var previous:Long?=null
            var remainder=0L
            while(model.state.value.running && model.state.value.feedbackRemaining>0)withFrameNanos { now ->
                previous?.let { before -> val elapsed=now-before+remainder;model.tick(elapsed/1_000_000);remainder=elapsed%1_000_000 }
                previous=now
            }
        }
    }
}

@Composable fun ConjugateScreen(s:QuizState,q:QuizQuestion,language:AppLanguage,
    onHome:()->Unit,onLetter:(Char)->Unit,onClear:()->Unit,onSubmit:(Int?)->Unit,onNext:()->Unit,onEdit:(Int,Int)->Unit={_,_->}) {
    val english=language==AppLanguage.ENGLISH_UK
    val config=androidx.compose.ui.platform.LocalConfiguration.current
    val shortLandscape=config.screenHeightDp<=360 || (config.screenWidthDp>config.screenHeightDp && LocalDensity.current.fontScale in 1.01f..1.5f)
    val tenseLabel=stringResource(ChoiceResources.french.getValue(Choices.tenses[q.tense]))
    Scaffold(containerColor=WarmBackground,contentWindowInsets=WindowInsets.safeDrawing,topBar={
        Surface(color=LearningBlue,contentColor=Color.White) {
            Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top+WindowInsetsSides.Horizontal)).heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onHome,modifier=Modifier.testTag("quiz_home")){Icon(Icons.Default.Home,if(english)"Home" else "Accueil")}
                if(shortLandscape) Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically) {
                    Text(if(english)"Conjugate" else "Conjuguer",Modifier.testTag("quiz_title"),fontSize=18.sp,fontWeight=FontWeight.Bold)
                    Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(q.verb.id+if(english)" / ${q.verb.meaning}"else "",Modifier.testTag("quiz_verb"),fontSize=17.sp,lineHeight=20.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                        Text(tenseLabel+if(english)" (${stringResource(ChoiceResources.tense.getValue(Choices.tenses[q.tense]))})"else "",Modifier.testTag("quiz_tense"),fontSize=14.sp,lineHeight=18.sp,textAlign=TextAlign.Center)
                    }
                } else Text(if(english)"Conjugate" else "Conjuguer",Modifier.weight(1f).testTag("quiz_title"),fontSize=22.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                Text("${s.progress}%",Modifier.widthIn(min=48.dp).padding(end=12.dp).testTag("quiz_progress"),fontSize=14.sp,textAlign=TextAlign.End)
            }
        }
    }) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
            val landscape=maxWidth>maxHeight
            val compact=if(landscape) maxHeight<400.dp else maxHeight<700.dp || (maxHeight<850.dp && (q.boxCount>10 || (q.tense in listOf(3,5) && q.boxCount>6)))
            val tightScaledPortrait=!landscape && LocalDensity.current.fontScale in 1.01f..1.5f && q.boxCount>=14
            val gap=if(tightScaledPortrait)2.dp else if(compact && !landscape)6.dp else if(compact)4.dp else 14.dp
            val completionGap=minOf(maxHeight*.1f,28.dp)
            val windowWidth=maxWidth
            Column(Modifier.fillMaxSize().padding(horizontal=if(compact)12.dp else 16.dp,vertical=if(tightScaledPortrait)0.dp else if(compact)4.dp else 10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                @Composable fun Heading() {
                    Text(q.verb.id+if(english)" / ${q.verb.meaning}" else "",Modifier.fillMaxWidth().testTag("quiz_verb"),color=SuccessGreen,
                    fontSize=if(compact && landscape)20.sp else if(compact)23.sp else 28.sp,lineHeight=if(compact && landscape)24.sp else if(compact)27.sp else 34.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                val tense=stringResource(ChoiceResources.french.getValue(Choices.tenses[q.tense]))+
                    if(english)" (${stringResource(ChoiceResources.tense.getValue(Choices.tenses[q.tense]))})" else ""
                Text(tense,Modifier.fillMaxWidth().testTag("quiz_tense"),color=LearningBlue,fontSize=if(compact && landscape)15.sp else if(compact)17.sp else 20.sp,lineHeight=if(compact && landscape)18.sp else if(compact)21.sp else 26.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
                Spacer(Modifier.height(gap))
                }
                if(landscape) {
                    val measure=rememberTextMeasurer()
                    val density=LocalDensity.current
                    val fixedStyle=LocalTextStyle.current.copy(fontSize=if(compact && androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<=360)17.sp else if(compact)19.5.sp else 21.5.sp,lineHeight=if(compact && androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<=360)20.sp else if(compact)24.sp else 28.sp)
                    val prefix=with(density){q.rows.maxOf{measure.measure(it.displaySubject+it.prefix,fixedStyle,softWrap=false).size.width}.toDp()}
                    val suffix=with(density){q.rows.maxOf{measure.measure(it.displaySuffix,fixedStyle,softWrap=false).size.width}.toDp()}
                    val available=minOf(windowWidth-(if(compact)24.dp else 32.dp),1250.dp)-gap
                    val minimumBox=with(density){17.sp.toDp()}+6.dp
                    val required=32.dp+prefix+6.dp+q.boxCount*minimumBox+(q.boxCount-1)*2.dp+suffix+(if(compact)38.dp else 42.dp)+10.dp
                    // Borrow only the width a single answer line needs; retain a usable keypad.
                    val answerWidth=maxOf(available/2,minOf(required,available-(if(shortLandscape && (q.keys.size>12 || density.fontScale>1f))200.dp else if(shortLandscape)160.dp else if(compact && (windowWidth<670.dp || density.fontScale>1f))200.dp else 240.dp)))
                    Row(Modifier.weight(1f).widthIn(max=1250.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(gap)) {
                        AnswerPanel(s,q,english,compact,onSubmit,onEdit,Modifier.width(answerWidth).fillMaxHeight(),landscape=true)
                        Column(Modifier.weight(1f).fillMaxHeight().testTag("quiz_right_column").verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Top) {
                            if(!shortLandscape)Heading()
                            if(s.done)Spacer(Modifier.height(completionGap))
                            Column(Modifier.widthIn(max=360.dp).fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
                                QuizControls(s,q,english,compact,onLetter,onClear,{onSubmit(null)},onNext,onHome,landscape=true)
                            }
                        }
                    }
                } else {
                    Heading()
                    Column(Modifier.weight(1f).widthIn(max=760.dp).fillMaxWidth().verticalScroll(rememberScrollState()).testTag("quiz_scroll"),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(gap)) {
                        AnswerPanel(s,q,english,compact,onSubmit,onEdit,Modifier.fillMaxWidth())
                        Column(Modifier.widthIn(max=360.dp).fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
                            QuizControls(s,q,english,compact,onLetter,onClear,{onSubmit(null)},onNext,onHome)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun AnswerPanel(s:QuizState,q:QuizQuestion,english:Boolean,compact:Boolean,onSubmit:(Int?)->Unit,onEdit:(Int,Int)->Unit,modifier:Modifier,landscape:Boolean=false) {
    val measurer=rememberTextMeasurer()
    val density=LocalDensity.current
    val textStyle=LocalTextStyle.current.copy(fontSize=if(landscape){if(compact && androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<=360)17.sp else if(compact)19.5.sp else 21.5.sp}else if(compact)17.sp else 20.sp,lineHeight=if(landscape){if(compact && androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<=360)20.sp else if(compact)24.sp else 28.sp}else if(compact)22.sp else 26.sp)
    val prefixWidth=with(density){q.rows.maxOf{measurer.measure(it.displaySubject+it.prefix,textStyle,softWrap=false).size.width}.toDp()}
    val suffixWidth=with(density){q.rows.maxOf{measurer.measure(it.displaySuffix,textStyle,softWrap=false).size.width}.toDp()}
    Surface(modifier.testTag("quiz_panel"),shape=RoundedCornerShape(20.dp),color=Color.White,shadowElevation=2.dp) {
        Column(Modifier.then(if(landscape)Modifier.fillMaxHeight().verticalScroll(rememberScrollState()) else Modifier).padding(horizontal=if(landscape && androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<=360)8.dp else if(landscape)16.dp else 10.dp,vertical=if(!landscape && compact && density.fontScale in 1.01f..1.5f && q.boxCount>=14)2.dp else if((landscape && (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<=360 || density.fontScale in 1.01f..1.5f)) || (compact && density.fontScale in 1.01f..1.5f && q.boxCount>=14))4.dp else if(compact)6.dp else 10.dp),verticalArrangement=Arrangement.spacedBy(if((landscape && (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<=360 || density.fontScale in 1.01f..1.5f)) || (compact && density.fontScale in 1.01f..1.5f && q.boxCount>=14))1.dp else if(compact)2.dp else 6.dp,if(landscape)Alignment.CenterVertically else Alignment.Top)) {
            q.rows.forEachIndexed { row,answer -> AnswerRow(row,answer,q.boxCount,prefixWidth,suffixWidth,s,english,compact,landscape,measurer,{onEdit(row,it)},{onSubmit(row)},{onSubmit(null)}) }
        }
    }
}

@Composable private fun AnswerRow(row:Int,answer:QuizAnswer,count:Int,prefixWidth:Dp,suffixWidth:Dp,s:QuizState,english:Boolean,compact:Boolean,landscape:Boolean,measurer:androidx.compose.ui.text.TextMeasurer,onEdit:(Int)->Unit,onSelect:()->Unit,onNext:()->Unit) {
    val active=s.active==row && !s.done
    val complete=row in s.locked
    val wrong=active && s.feedbackRemaining>0
    val density=LocalDensity.current
    val size=if(landscape){if(compact && androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<=360)17.sp else if(compact)19.5.sp else 21.5.sp}else if(compact)17.sp else 20.sp
    val fixedStyle=LocalTextStyle.current.copy(fontSize=size,lineHeight=if(landscape){if(compact && androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<=360)20.sp else if(compact)24.sp else 28.sp}else if(compact)22.sp else 26.sp).let { style -> if(density.fontScale in 1.01f..1.5f)style.copy(platformStyle=androidx.compose.ui.text.PlatformTextStyle(includeFontPadding=false),lineHeightStyle=androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,androidx.compose.ui.text.style.LineHeightStyle.Trim.Both))else style }
    val prefixGap=6.dp
    val rowHeight=if(landscape){if(compact && androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<=360)34.dp else if(compact && density.fontScale in 1.01f..1.5f)38.dp else if(compact)41.dp else 47.dp}else if(compact)38.dp else 44.dp
    BoxWithConstraints(Modifier.fillMaxWidth().clickable(enabled=s.canEdit && !complete,onClick=onSelect)
        .testTag("quiz_row_$row").semantics{stateDescription=if(complete){if(english)"Correct" else "Correct"}else if(wrong){if(english)"Try again" else "Essaie encore"}else if(active){if(english)"Active row" else "Ligne active"}else{if(english)"Unfinished" else "À compléter"}}) {
        val nextWidth=if(density.fontScale>1.5f)48.dp else if(landscape && androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<=360)34.dp else if(compact)38.dp else 42.dp
        val minBox=if(density.fontScale>1.5f)with(density){measurer.measure("m",TextStyle(fontSize=19.sp,fontWeight=FontWeight.SemiBold),softWrap=false).size.width.toDp()}+6.dp else if(compact)with(density){measurer.measure("m",TextStyle(fontSize=16.sp,fontWeight=FontWeight.SemiBold),softWrap=false).size.width.toDp()}+1.dp
            else with(density){(if(landscape)17.sp else 16.sp).toDp()}+6.dp
        val desiredBox=if(density.fontScale>1.5f)minBox+4.dp else if(landscape){if(compact)30.dp else 34.dp}else 32.dp
        val rowWidth=maxWidth
        // Long forms retain one aligned box row; let the fixed pronoun/auxiliary wrap
        // inside its own column before stacking the entire answer on another line.
        val fittedPrefix=if(compact && density.fontScale<=1.5f)minOf(prefixWidth,(rowWidth-prefixGap-count*minBox-(count-1)*2.dp-suffixWidth-nextWidth-8.dp).coerceAtLeast(48.dp)) else prefixWidth
        val twoLines=rowWidth < fittedPrefix+prefixGap+count*minBox+(count-1)*2.dp+suffixWidth+nextWidth+8.dp
        @Composable fun FixedText() {
            Text((answer.displaySubject+answer.prefix).trimEnd(),Modifier.then(if(twoLines)Modifier else Modifier.width(fittedPrefix)).testTag("quiz_fixed_$row"),style=fixedStyle,textAlign=if(twoLines)TextAlign.Start else TextAlign.End,softWrap=twoLines || fittedPrefix<prefixWidth,color=DeepNavyInk)
        }
        @Composable fun Boxes(available:Dp) {
            val slots=((available-nextWidth-suffixWidth-6.dp)/(minBox+2.dp)).toInt().coerceIn(1,count)
            val boxWidth=minOf(desiredBox,(available-nextWidth-suffixWidth-6.dp-(slots-1)*2.dp)/slots).coerceAtLeast(16.dp)
            Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
                (0 until count).toList().chunked(slots).forEachIndexed { line,indices ->
                    val finalLine=indices.last()==count-1
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                        indices.forEach { index -> LetterBox(s.entries[row].getOrNull(index)?.toString().orEmpty(),boxWidth,if(landscape){if(compact)32.dp else 36.dp}else 34.dp,landscape,
                            complete,wrong,index<s.hints[row].length,active && !wrong && index==s.entries[row].length,"quiz_box_${row}_$index",measurer,active,s.canEdit,{onEdit(index)},if(english)"Edit letter ${index+1}" else "Modifier la lettre ${index+1}") }
                        if(finalLine) {
                            if(suffixWidth>0.dp)Text(answer.displaySuffix,Modifier.width(suffixWidth),fontSize=size,color=DeepNavyInk)
                            Spacer(Modifier.width(2.dp))
                            if(active)ActiveRowNext(english,s.canEdit,onNext,nextWidth) else Spacer(Modifier.width(nextWidth))
                        }
                    }
                }
            }
        }
        if(twoLines) {
            Column(Modifier.padding(vertical=if(compact && !landscape)0.dp else 2.dp)) {
                FixedText()
                Boxes(rowWidth)
            }
        } else {
            Row(Modifier.heightIn(min=rowHeight),verticalAlignment=Alignment.CenterVertically) {
                FixedText()
                Spacer(Modifier.width(prefixGap))
                Boxes(rowWidth-fittedPrefix-prefixGap)
            }
        }
    }
}

@Composable internal fun LetterBox(letter:String,width:Dp,height:Dp,landscape:Boolean,complete:Boolean,wrong:Boolean,hinted:Boolean,active:Boolean,tag:String,measurer:androidx.compose.ui.text.TextMeasurer,editableRow:Boolean,enabled:Boolean,onEdit:()->Unit,editLabel:String) {
    val density=LocalDensity.current
    val normalSize=if(width<24.dp)18f else if(landscape)20.5f else 19f
    val letterSize=remember(width,normalSize,density,measurer) {
        val available=with(density){(width-2.dp).toPx()}
        var size=normalSize
        while(size>12f && measurer.measure("m",TextStyle(fontSize=size.sp,fontWeight=FontWeight.SemiBold),softWrap=false).size.width>available)size-=.5f
        size.sp
    }
    val answerState=stringResource(when {
        wrong->com.templesoftware.frenchverbs.R.string.answer_incorrect
        hinted->com.templesoftware.frenchverbs.R.string.answer_hint
        complete->com.templesoftware.frenchverbs.R.string.answer_correct
        active->com.templesoftware.frenchverbs.R.string.answer_active
        letter.isNotEmpty()->com.templesoftware.frenchverbs.R.string.answer_entered
        else->com.templesoftware.frenchverbs.R.string.answer_empty
    })
    val orange=Color(0xFFB45309)
    val background=when {wrong->WarmError;hinted->orange;complete->SuccessGreen;else->Color.White}
    val border=when {active->CelebrationGold;wrong->WarmError;hinted->orange;complete->SuccessGreen;else->SoftOutline}
    Box(Modifier.width(width).heightIn(min=height).background(background,RoundedCornerShape(5.dp))
        .border(if(active)2.dp else 1.dp,border,RoundedCornerShape(5.dp)).testTag(tag)
        .then(if(editableRow)Modifier.clickable(enabled=enabled,onClickLabel=editLabel,onClick=onEdit) else Modifier)
        .semantics {stateDescription=answerState},contentAlignment=Alignment.Center) {
        Text(letter,Modifier.fillMaxWidth().testTag("${tag}_letter"),fontSize=letterSize,lineHeight=letterSize*1.2f,maxLines=1,softWrap=false,fontWeight=FontWeight.SemiBold,color=if(complete||wrong||hinted)Color.White else DeepNavyInk,textAlign=TextAlign.Center)
    }
}

@Composable private fun ActiveRowNext(english:Boolean,enabled:Boolean,onClick:()->Unit,width:Dp) {
    Surface(onClick=onClick,enabled=enabled,color=if(enabled)SuccessGreen else SuccessGreen.copy(alpha=.4f),contentColor=Color.White,
        shape=RoundedCornerShape(10.dp),modifier=Modifier.then(if(LocalDensity.current.fontScale>1.5f)Modifier.width(width).heightIn(min=maxOf(width,with(LocalDensity.current){28.sp.toDp()}+8.dp))else Modifier.size(width)).testTag("quiz_row_next").semantics{contentDescription=if(english)"Next" else "Suivant"}) {
        Box(contentAlignment=Alignment.Center){Text("↓",fontSize=if(width<38.dp)22.sp else 26.sp,lineHeight=if(width<38.dp)24.sp else 28.sp)}
    }
}

@Composable private fun GreenButton(label:String,tag:String,enabled:Boolean=true,onClick:()->Unit,modifier:Modifier=Modifier,singleLine:Boolean=false,minimumHeight:Dp=48.dp) {
    PremiumGradientButton(onClick,Color(0xFF389664),SuccessGreen,Color.White.copy(alpha=.2f),modifier.heightIn(min=minimumHeight,max=if(minimumHeight<48.dp && LocalDensity.current.fontScale<=1.3f)maxOf(minimumHeight,with(LocalDensity.current){22.sp.toDp()}+12.dp)else Dp.Infinity).testTag(tag),
        enabled=enabled,contentPadding=PaddingValues(horizontal=if(minimumHeight<48.dp)4.dp else 8.dp,vertical=6.dp)) {
        Text(label,modifier=if(singleLine)Modifier.fillMaxWidth()else Modifier,maxLines=if(singleLine)1 else Int.MAX_VALUE,softWrap=!singleLine,color=Color.White,fontSize=18.sp,lineHeight=22.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
    }
}

@Composable private fun QuizControls(s:QuizState,q:QuizQuestion,english:Boolean,compact:Boolean,onLetter:(Char)->Unit,onClear:()->Unit,onEnter:()->Unit,onNext:()->Unit,onHome:()->Unit,landscape:Boolean=false) {
    if(s.done) {
        Column(Modifier.fillMaxWidth().testTag("quiz_success"),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(if(s.last){if(english)"Well done! You’ve finished." else "Bravo ! Tu as terminé."}else{if(english)"Well done!" else "Bravo !"},color=SuccessGreen,fontSize=22.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
            GreenButton(if(s.last){if(english)"Home" else "Accueil"}else{if(landscape){if(english)"Next →" else "Suite →"}else{if(english)"Next question" else "Question suivante"}},if(s.last)"quiz_finish" else "quiz_next_question",onClick=if(s.last)onHome else onNext,modifier=Modifier.fillMaxWidth(),singleLine=landscape)
        }
    } else {
        LetterKeypad(q.keys,english,compact,s.canEdit,onLetter,onClear,onEnter,keyHeight=if(compact && landscape)40.dp else 48.dp)
    }
}

/** Presentation only: required letters and validation remain owned by the quiz model. */
@Composable internal fun LetterKeypad(keys:List<Char>,english:Boolean,compact:Boolean,enabled:Boolean,
    onLetter:(Char)->Unit,onClear:()->Unit,onEnter:()->Unit,keyHeight:Dp=48.dp,inlineClear:Boolean=false) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val preferredColumns=if(compact && LocalDensity.current.fontScale in 1.01f..1.5f && keys.size in 10..12 && maxWidth>=340.dp)6 else if(compact && keys.size>12 && (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<=360 || LocalDensity.current.fontScale>1f || WindowInsets.navigationBars.getBottom(LocalDensity.current)>with(LocalDensity.current){30.dp.toPx()}))5 else if(compact && keys.size>9)4 else 3
    val columns=minOf(preferredColumns,((maxWidth+8.dp)/48.dp).toInt().coerceAtLeast(1))
    val remaining=if(keys.size>9)(columns-keys.size%columns)%columns else 0
    // A single narrow fourth-column cell cannot hold translated action labels.
    val spare=if(remaining==1 && maxWidth/columns<100.dp)0 else remaining
    val enter=if(english)"Enter" else "Valider"
    val clear=if(english)"Clear" else "Effacer"
    val inlineLabel=if(inlineClear)clear else enter
    val inlineTag=if(inlineClear)"quiz_clear"else "quiz_enter"
    val inlineAction=if(inlineClear)onClear else onEnter
        val cell=(maxWidth-8.dp*(columns-1))/columns
        val clearWidth=minOf(176.dp,(maxWidth-8.dp)/2)
        Column(Modifier.fillMaxWidth().testTag("quiz_keypad"),verticalArrangement=Arrangement.spacedBy(if(compact)4.dp else 8.dp)) {
            val rows=keys.chunked(columns)
            rows.forEachIndexed { index,row ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(index==rows.lastIndex && spare>0) {
                        row.forEach{c->GreenButton(c.toString(),"quiz_key_$c",enabled,{onLetter(c)},Modifier.width(cell),minimumHeight=keyHeight)}
                        GreenButton(inlineLabel,inlineTag,enabled,inlineAction,Modifier.width(cell*spare+8.dp*(spare-1)),minimumHeight=keyHeight)
                    } else {
                        row.forEach{c->GreenButton(c.toString(),"quiz_key_$c",enabled,{onLetter(c)},Modifier.weight(1f),minimumHeight=keyHeight)}
                        repeat(columns-row.size){Spacer(Modifier.weight(1f))}
                    }
                }
            }
            Spacer(Modifier.height(if(compact)2.dp else 4.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=if(spare>0)Arrangement.Center else Arrangement.spacedBy(8.dp)) {
                GreenButton(if(spare>0&&inlineClear)enter else clear,if(spare>0&&inlineClear)"quiz_enter"else "quiz_clear",enabled,if(spare>0&&inlineClear)onEnter else onClear,
                    if(spare>0)Modifier.width(clearWidth) else Modifier.weight(1f),minimumHeight=keyHeight)
                if(spare==0)GreenButton(enter,"quiz_enter",enabled,onEnter,Modifier.weight(1f),minimumHeight=keyHeight)
            }
        }
    }
}
