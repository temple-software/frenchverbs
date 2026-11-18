package com.templesoftware.frenchverbs.ui.use

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.use.*
import com.templesoftware.frenchverbs.domain.verbs.Choices
import com.templesoftware.frenchverbs.ui.app.UseViewModel
import com.templesoftware.frenchverbs.ui.quiz.*
import com.templesoftware.frenchverbs.ui.theme.*
import com.templesoftware.frenchverbs.ui.verbs.ChoiceResources

/** A single lifecycle-bound clock for both feedback colours; saved remaining time resumes. */
@Composable fun UseFeedbackClock(model:UseViewModel) {
    val s by model.state.collectAsState();val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(model,lifecycle,s.running,s.pending) {
        if(s.running&&s.pending)lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var previous:Long?=null;var remainder=0L
            while(model.state.value.running&&model.state.value.pending)withFrameNanos { now ->
                previous?.let{val d=now-it+remainder;model.tick(d/1_000_000);remainder=d%1_000_000};previous=now
            }
        }
    }
}

@Composable fun UseScreen(s:UseState,language:AppLanguage,onBack:()->Unit,onLetter:(Char)->Unit,onClear:()->Unit,onEnter:()->Unit,onEdit:(Int)->Unit,onPause:(Boolean)->Unit={},letterReady:Boolean=true) {
    val english=language==AppLanguage.ENGLISH_UK
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    fun requestBack(){if(s.complete)onBack()else{confirmExit=true;onPause(true)}}
    fun stay(){confirmExit=false;onPause(false)}
    LaunchedEffect(confirmExit){onPause(confirmExit)}
    BackHandler(enabled=!s.complete){requestBack()}
    if(confirmExit)AlertDialog(onDismissRequest={stay()},title={Text(if(english)"Leave the quiz?"else "Quitter le quiz ?")},
        text={Text(if(english)"Are you sure? Your progress in this quiz will be lost."else "Veux-tu vraiment quitter ? Ta progression dans ce quiz sera perdue.")},
        confirmButton={TextButton(onClick=onBack,modifier=Modifier.testTag("use_exit_confirm")){Text(if(english)"Leave"else "Quitter")}},
        dismissButton={TextButton(onClick={stay()},modifier=Modifier.testTag("use_exit_stay")){Text(if(english)"Keep playing"else "Continuer")}})
    val q=s.current
    val title=if(q==null){if(english)"Finished"else "Terminé"}else q.verbId+" - "+stringResource(ChoiceResources.french.getValue(Choices.tenses[q.tense])).replaceFirstChar{it.lowercase()}
    Scaffold(contentWindowInsets=WindowInsets.safeDrawing,topBar={
        Surface(color=LearningBlue,contentColor=Color.White) {
            Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top+WindowInsetsSides.Horizontal)).heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={requestBack()},modifier=Modifier.testTag("use_back")){Icon(Icons.AutoMirrored.Filled.ArrowBack,if(english)"Back"else "Retour")}
                BoxWithConstraints(Modifier.weight(1f)) {
                    val measure=rememberTextMeasurer();val density=LocalDensity.current
                    val size=remember(title,maxWidth,density){(22 downTo 16).firstOrNull{measure.measure(title,TextStyle(fontSize=it.sp,fontWeight=FontWeight.Bold),softWrap=false).size.width<=with(density){maxWidth.toPx()}}?:16}
                    Text(title,Modifier.fillMaxWidth().testTag("use_title"),fontSize=size.sp,lineHeight=(size+4).sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,maxLines=2)
                }
                Spacer(Modifier.width(48.dp))
            }
        }
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets),horizontalAlignment=Alignment.CenterHorizontally) {
            UseScore(s,english)
            if(q==null) {
                UseResults(s,english,onBack)
            } else BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=if(LocalDensity.current.fontScale>1f)6.dp else 8.dp)) {
                val landscape=maxWidth>maxHeight;val height=maxHeight
                val compactPortrait=!landscape && height<650.dp && q.keys.size>9
                @Composable fun Keypad() {Box(Modifier.widthIn(max=340.dp).fillMaxWidth()){LetterKeypad(q.keys,english,landscape || compactPortrait,s.copy(running=true).canEdit&&letterReady,{if(s.canEdit&&!confirmExit)onLetter(it)},{if(s.canEdit&&!confirmExit)onClear()},{if(s.canEdit&&!confirmExit)onEnter()},keyHeight=if(landscape&&height<280.dp)40.dp else 48.dp,inlineClear=true)}}
                if(landscape) {
                    Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(if(height<220.dp)12.dp else 24.dp)) {
                        Column(Modifier.weight(1.1f).fillMaxHeight().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.Center){SentencePanel(s,q,english,true,onEdit,compact=height<220.dp)}
                        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Keypad()}
                    }
                } else {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally) {
                        Column(Modifier.heightIn(min=height).widthIn(max=680.dp).fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceEvenly) {
                            SentencePanel(s,q,english,false,onEdit)
                            Spacer(Modifier.height(20.dp))
                            Keypad()
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun UseScore(s:UseState,english:Boolean) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),contentAlignment=Alignment.Center) {
        val circle=minOf(24.dp,(maxWidth-9*6.dp)/10)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.testTag("use_scores")) {
            s.outcomes.forEachIndexed { i,outcome ->
                val active=i==s.index
                val base=when(outcome){1->SuccessGreen;-1->WarmError;else->Color(0xFFFFF8E8)}
                val status=if(english)when(outcome){1->"Correct first attempt";-1->"Retry";else->"Unanswered"}else when(outcome){1->"Réussi du premier coup";-1->"Nouvel essai";else->"Sans réponse"}
                Box(Modifier.size(circle).shadow(1.dp,CircleShape).background(Brush.verticalGradient(listOf(lerp(base,Color.White,.48f),base)),CircleShape)
                    .border(if(active)2.dp else 1.dp,if(active)CelebrationGold else SoftOutline,CircleShape).testTag("use_score_$i")
                    .semantics{contentDescription="${i+1}: $status";stateDescription=status;selected=active})
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun SentencePanel(s:UseState,q:UseQuestion,english:Boolean,landscape:Boolean,onEdit:(Int)->Unit,compact:Boolean=false) {
    val measurer=rememberTextMeasurer();val density=LocalDensity.current
    val width=maxOf(if(compact && q.boxCount>14)28.dp else 32.dp,with(density){20.sp.toDp()}+8.dp)
    val entry=s.input.entries[q.person];val hint=s.input.hints[q.person]
    val wrong=s.input.feedbackRemaining>0;val correct=s.successRemaining>0
    val fixed=remember(q.form){buildMap<Int,String>{var letters=0;q.form.forEach{if(it.isLetter())letters++ else put(letters,get(letters).orEmpty()+it)}}}
    val display=remember(q.form,entry){useDisplayEntry(q.form,entry)}
    val hintDisplay=remember(q.form,hint){useDisplayEntry(q.form,hint)}
    val caret=useCursorAlpha(s.canEdit)
    var rendered by remember(q){mutableStateOf<TextLayoutResult?>(null)}
    val cursorIndex=q.prefix.length+display.length
    val sentence=buildAnnotatedString {
        append(q.prefix)
        val color=when{wrong->WarmError;correct->SuccessGreen;else->DeepNavyInk}
        withStyle(SpanStyle(color=color,fontWeight=FontWeight.SemiBold)) {
            if(!wrong&&!correct&&hintDisplay.isNotEmpty()) {
                withStyle(SpanStyle(color=Color(0xFFB45309))){append(display.take(hintDisplay.length))}
                append(display.drop(hintDisplay.length))
            } else append(display)
        }
        // A thin fixed-width space reserves the caret position even while it is hidden.
        append("\u200A")
        append(q.suffix)
    }
    Column(Modifier.fillMaxWidth().testTag("use_entry_group"),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(if(compact)6.dp else if(landscape)10.dp else 16.dp)) {
        Surface(shape=RoundedCornerShape(20.dp),color=Color.White,shadowElevation=2.dp,modifier=Modifier.fillMaxWidth().testTag("use_sentence")) {
            Text(sentence,Modifier.padding(if(compact)8.dp else if(landscape)12.dp else 20.dp).fillMaxWidth().testTag("use_live_sentence")
                .drawWithContent {
                    drawContent()
                    if(s.canEdit)rendered?.takeIf{it.layoutInput.text==sentence}?.let {
                        val rect=it.getBoundingBox(cursorIndex)
                        drawLine(LearningBlue.copy(alpha=caret.value),Offset(rect.left,rect.top+3.dp.toPx()),Offset(rect.left,rect.bottom-3.dp.toPx()),strokeWidth=2.dp.toPx())
                    }
                }.clearAndSetSemantics {
                    text=AnnotatedString(q.prefix+display+q.suffix)
                    stateDescription=when{wrong->if(english)"Incorrect"else "Incorrect";correct->if(english)"Correct"else "Correct";else->if(english)"Answer entry"else "Saisie de la réponse"}
                    getTextLayoutResult{results->rendered?.let{results.add(it);true}?:false}
                },fontSize=if(compact)20.sp else 22.sp,lineHeight=if(compact)26.sp else 32.sp,color=DeepNavyInk,onTextLayout={rendered=it})
        }
        FlowRow(Modifier.fillMaxWidth().testTag("use_answer_boxes"),horizontalArrangement=Arrangement.spacedBy(4.dp,Alignment.CenterHorizontally),verticalArrangement=Arrangement.spacedBy(if(compact)4.dp else 8.dp)) {
            for(i in 0 until q.boxCount) {
                fixed[i]?.let{Text(if(it.isBlank())" "else it,fontSize=22.sp,lineHeight=38.sp,modifier=Modifier.widthIn(min=if(it.isBlank())10.dp else 0.dp))}
                LetterBox(entry.getOrNull(i)?.toString().orEmpty(),width,38.dp,landscape,correct,wrong,i<hint.length,
                    s.canEdit&&i==entry.length,"use_box_$i",measurer,true,s.canEdit,{onEdit(i)},if(english)"Edit letter ${i+1}"else "Modifier la lettre ${i+1}")
            }
        }
    }
}

@Composable private fun UseResults(s:UseState,english:Boolean,onHome:()->Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("use_finished")) {
        val landscape=maxWidth>maxHeight
        val reviewCount=s.outcomes.count{it==-1}
        val collapsible=landscape || reviewCount>3 || LocalDensity.current.fontScale>1.3f
        var expanded by rememberSaveable { mutableStateOf(false) }
        val starSize=minOf(if(landscape)48.dp else 68.dp,(maxWidth-64.dp)/5)
        Column(Modifier.fillMaxSize().padding(horizontal=24.dp,vertical=if(landscape)8.dp else 24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            val stars=useStars(s.score)
            Row(Modifier.testTag("use_stars").semantics{contentDescription=if(english)"$stars out of 5 stars"else "$stars étoiles sur 5"},horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                repeat(5){i->Icon(if(i<stars)Icons.Filled.Star else Icons.Outlined.Star,null,tint=if(i<stars)CelebrationGold else SoftOutline,modifier=Modifier.size(starSize))}
            }
            Spacer(Modifier.height(if(landscape)8.dp else 32.dp))
            if(collapsible && reviewCount>0)TextButton(onClick={expanded=!expanded},modifier=Modifier.testTag("use_review_toggle")){Text(if(expanded){if(english)"Hide mistakes"else "Masquer les erreurs"}else conjugateErrorSummary(reviewCount,english),fontSize=16.sp)}
            Column(Modifier.weight(1f,fill=false).widthIn(max=720.dp).fillMaxWidth().then(if(expanded)Modifier.verticalScroll(rememberScrollState())else Modifier),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                s.questions.forEachIndexed { i,q -> if(s.outcomes[i]==-1 && (!collapsible || expanded)) {
                    val tense=if(s.selectedTenseCount>1)stringResource(ChoiceResources.french.getValue(Choices.tenses[q.tense])).replaceFirstChar{it.lowercase()}else ""
                    Row(Modifier.fillMaxWidth().testTag("use_review_$i"),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text("✕",color=WarmError,fontSize=24.sp)
                        Text(buildAnnotatedString{append(q.conjugation);if(tense.isNotEmpty())withStyle(SpanStyle(color=Color.Gray)){append(" ($tense)")}},fontSize=22.sp,lineHeight=30.sp,color=DeepNavyInk)
                    }
                } }
            }
            Spacer(Modifier.height(if(landscape)8.dp else 28.dp))
            Button(onClick=onHome,modifier=Modifier.testTag("use_home")){Text(if(english)"Home"else "Accueil")}
        }
    }
}
