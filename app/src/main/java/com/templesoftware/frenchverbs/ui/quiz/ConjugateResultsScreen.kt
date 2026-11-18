package com.templesoftware.frenchverbs.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.quiz.*
import com.templesoftware.frenchverbs.domain.verbs.Choices
import com.templesoftware.frenchverbs.ui.app.ConjugateViewModel
import com.templesoftware.frenchverbs.ui.theme.*
import com.templesoftware.frenchverbs.ui.verbs.ChoiceResources

/** Shared by production navigation and the debug fixture; no new quiz or completion route. */
@Composable fun ConjugateActivityContent(model:ConjugateViewModel,language:AppLanguage,onHome:()->Unit) {
    val s by model.state.collectAsState()
    if(s.done && s.last && s.completedLines.size==s.session.size*6) {
        ConjugateResultsScreen(s,language,model::toggleResult,onHome)
    } else {
        QuizFeedbackClock(model)
        model.question?.let{ConjugateScreen(s,it,language,onHome,model::letter,model::clear,model::submit,model::nextQuestion,model::edit)}
    }
}

fun conjugateErrorSummary(count:Int,english:Boolean)=if(english) {
    if(count==1)"1 wrong answer — press to see it" else "$count wrong answers — press to see them"
} else {
    if(count==1)"1 erreur — appuyez pour la voir" else "$count erreurs — appuyez pour les voir"
}

@Composable fun ConjugateResultsScreen(s:QuizState,language:AppLanguage,onToggle:(Int)->Unit,onHome:()->Unit) {
    val english=language==AppLanguage.ENGLISH_UK
    val results=remember(s.completedLines,s.wrongLines){ConjugateResults(s)}
    val score=results.score
    BackHandler(onBack=onHome)
    Scaffold(containerColor=WarmBackground,contentWindowInsets=WindowInsets.safeDrawing,topBar={
        Surface(color=LearningBlue,contentColor=Color.White) {
            Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top+WindowInsetsSides.Horizontal)).heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onHome,modifier=Modifier.testTag("conj_results_back")){Icon(Icons.AutoMirrored.Filled.ArrowBack,if(english)"Back"else "Retour")}
                Text(if(english)"Results"else "Résultats",Modifier.weight(1f).testTag("conj_results_title"),fontSize=22.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                Spacer(Modifier.width(48.dp))
            }
        }
    }) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).testTag("conj_results"),contentAlignment=Alignment.TopCenter) {
            val landscape=maxWidth>maxHeight
            val compact=results.groups.size>2 && maxHeight<700.dp
            val short=landscape && (maxHeight<260.dp || (maxHeight<400.dp && LocalDensity.current.fontScale>1f))
            val starSize=minOf(if(short)28.dp else if(compact)40.dp else 68.dp,(maxWidth-64.dp)/5)
            val contentWidth=minOf(maxWidth,if(landscape)1320.dp else 720.dp)-48.dp
            // Compact summaries can share three columns; enlarged review text keeps wider cards.
            val minimumCardWidth=if(short && LocalDensity.current.fontScale<=1.5f)160.dp else (if(compact)160.dp else 320.dp)*LocalDensity.current.fontScale.coerceAtLeast(1f)
            val columns=if(landscape)((contentWidth+16.dp)/(minimumCardWidth+16.dp)).toInt().coerceIn(1,3) else 1
            Column(Modifier.widthIn(max=if(landscape)1320.dp else 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=if(compact)4.dp else if(landscape)8.dp else 16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                // Match Utiliser's accepted filled/outline stars, colours and spacing exactly.
                Row(Modifier.testTag("conj_results_stars").semantics{contentDescription=if(english)"${score.stars} out of 5 stars"else "${score.stars} étoiles sur 5"},horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    repeat(5){i->Icon(if(i<score.stars)Icons.Filled.Star else Icons.Outlined.Star,null,tint=if(i<score.stars)CelebrationGold else SoftOutline,modifier=Modifier.size(starSize))}
                }
                Text(if(english)"${score.correctLines} / ${score.totalLines} correct"else "${score.correctLines} / ${score.totalLines} réponses correctes",Modifier.padding(top=if(landscape)4.dp else 8.dp).testTag("conj_results_score"),fontSize=if(short)16.sp else 18.sp,lineHeight=if(short)20.sp else 24.sp,color=DeepNavyInk,textAlign=TextAlign.Center)
                Spacer(Modifier.height(if(compact)4.dp else if(landscape)12.dp else 24.dp))
                if(results.groups.isEmpty())Text(if(english)"Perfect! No mistakes."else "Parfait ! Aucune erreur.",Modifier.testTag("conj_results_perfect"),color=SuccessGreen,fontSize=22.sp,textAlign=TextAlign.Center)
                @Composable fun TenseSection(tense:Int,lines:List<ConjugateResultLine>) {
                    val title=stringResource(ChoiceResources.french.getValue(Choices.tenses[tense]))
                    val expanded=results.initiallyExpanded(tense) xor (tense in s.resultToggles)
                    val summary=conjugateErrorSummary(lines.size,english).let{if(compact)it.substringBefore(" —") else it}
                    Column(Modifier.fillMaxWidth().padding(bottom=if(compact || landscape)0.dp else 12.dp).clickable(role=Role.Button,onClick={onToggle(tense)}).semantics(mergeDescendants=true) {
                        contentDescription="$title, ${summary.substringBefore(" —")}"
                        traversalIndex=tense.toFloat()
                        stateDescription=if(expanded){if(english)"Expanded"else "Développé"}else{if(english)"Collapsed"else "Réduit"}
                    }.testTag("conj_result_group_$tense").padding(horizontal=if(landscape)8.dp else 0.dp,vertical=if(short)2.dp else if(compact)4.dp else if(landscape)12.dp else 6.dp)) {
                        Row(Modifier.fillMaxWidth().heightIn(min=if(short)26.dp else if(compact)30.dp else 40.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(title,Modifier.weight(1f).testTag("conj_result_heading_$tense"),color=LearningBlue,fontSize=if(short)16.sp else if(compact)18.sp else 22.sp,lineHeight=if(short)20.sp else if(compact)22.sp else 28.sp,fontWeight=FontWeight.SemiBold)
                            Icon(Icons.Default.ArrowDropDown,null,Modifier.size(28.dp).rotate(if(expanded)180f else 0f),tint=LearningBlue)
                        }
                        if(landscape && !short)Spacer(Modifier.height(4.dp))
                        if(!expanded)Text(summary,Modifier.testTag("conj_result_summary_$tense"),color=WarmError,fontSize=if(compact)14.sp else 18.sp,lineHeight=if(compact)18.sp else 25.sp)
                        else lines.forEach { line ->
                            Row(Modifier.fillMaxWidth().padding(vertical=3.dp).testTag("conj_result_line_${line.id}"),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                Text("✕",color=WarmError,fontSize=22.sp)
                                Text(line.conjugation,Modifier.weight(1f).testTag("conj_result_text_${line.id}"),color=WarmError,fontSize=22.sp,lineHeight=30.sp)
                            }
                        }
                    }
                }
                if(landscape) {
                    val sections=results.groups.entries.toList()
                    val cardWidth=if(sections.size==1 || columns==1)minOf(contentWidth,540.dp*LocalDensity.current.fontScale.coerceAtLeast(1f))
                        else (contentWidth-16.dp*(columns-1))/columns
                    Column(Modifier.fillMaxWidth().testTag("conj_results_grid").semantics{isTraversalGroup=true},verticalArrangement=Arrangement.spacedBy(if(short)4.dp else if(compact)8.dp else 16.dp)) {
                        // Rows follow reading order. Natural-height cards never stretch to match a neighbour.
                        sections.chunked(columns).forEach { row ->
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp,Alignment.CenterHorizontally),verticalAlignment=Alignment.Top) {
                                row.forEach { (tense,lines) ->
                                    Surface(Modifier.width(cardWidth).testTag("conj_result_card_$tense"),shape=RoundedCornerShape(16.dp),color=Color.White,shadowElevation=1.dp) {
                                        TenseSection(tense,lines)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    results.groups.forEach { (tense,lines) -> TenseSection(tense,lines) }
                }
                Spacer(Modifier.height(if(compact)4.dp else 16.dp))
                Button(onClick=onHome,modifier=Modifier.testTag("conj_results_home")){Text(if(english)"Home"else "Accueil")}
            }
        }
    }
}
