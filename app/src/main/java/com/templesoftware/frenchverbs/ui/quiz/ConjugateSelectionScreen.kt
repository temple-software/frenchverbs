package com.templesoftware.frenchverbs.ui.quiz

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.templesoftware.frenchverbs.R
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.quiz.*
import com.templesoftware.frenchverbs.domain.verbs.Choices
import com.templesoftware.frenchverbs.ui.components.PremiumGradientButton
import com.templesoftware.frenchverbs.ui.theme.*
import com.templesoftware.frenchverbs.ui.verbs.ChoiceResources

@Composable fun ConjugateSelectionScreen(s:ConjugateSelection,language:AppLanguage,onHome:()->Unit,onBack:()->Unit,
    onTenses:()->Unit,onTypes:()->Unit,onRandom:()->Unit,onTense:(Int)->Unit,onCategory:(String)->Unit) {
    val english=language==AppLanguage.ENGLISH_UK
    val title=stringResource(when(s.page){
        ConjugateChoicePage.TENSE->R.string.conj_tense
        ConjugateChoicePage.TYPE->R.string.conj_type
        else->R.string.conj_today
    })
    @Composable fun tenseName(index:Int):String {
        val key=Choices.tenses[index]
        return stringResource(ChoiceResources.french.getValue(key))+
            if(english)" (${stringResource(ChoiceResources.tense.getValue(key))})"else ""
    }
    val heading=when(s.page) {
        ConjugateChoicePage.TODAY->stringResource(R.string.conj_focus)
        ConjugateChoicePage.TENSE->if(s.current.size<5)s.verbs.joinToString(", ") else
            stringResource(if(s.verbs.size==1)R.string.conj_one_verb else R.string.conj_many_verbs,verbCountWord(s.verbs.size,!english))
        ConjugateChoicePage.TYPE->s.currentTenses.map{tenseName(it)}.joinToString(" · ")
        else->stringResource(R.string.empty_session)
    }
    data class Option(val id:String,val text:String,val action:()->Unit)
    val options=when(s.page) {
        ConjugateChoicePage.TODAY->listOf(
            Option("tense",stringResource(R.string.conj_a_tense),onTenses),
            Option("type",stringResource(R.string.conj_a_type),onTypes),
            Option("random",stringResource(R.string.conj_random),onRandom))
        ConjugateChoicePage.TENSE->s.originalTenses.map{t->Option("tense_$t",tenseName(t)){onTense(t)}}
        ConjugateChoicePage.TYPE->s.categories.map{c->Option("type_$c",stringResource(when(c){
            "important"->R.string.conj_important;"er"->R.string.er_title;"ir"->R.string.ir_title;else->R.string.conj_irregular
        })){onCategory(c)}}
        else->emptyList()
    }
    Scaffold(containerColor=WarmBackground,contentWindowInsets=WindowInsets.safeDrawing,topBar={
        Surface(color=LearningBlue,contentColor=Color.White) {
            Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top+WindowInsetsSides.Horizontal)).heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onBack,Modifier.testTag("conj_select_back")){Icon(Icons.AutoMirrored.Filled.ArrowBack,if(english)"Back"else "Retour")}
                Text(title,Modifier.weight(1f).testTag("conj_select_title"),fontSize=22.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                IconButton(onHome,Modifier.testTag("conj_select_home")){Icon(Icons.Default.Home,if(english)"Home"else "Accueil")}
            }
        }
    }) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
            val landscape=maxWidth>maxHeight
            val short=if(landscape)maxHeight<360.dp else maxHeight<640.dp
            val height=maxHeight
            val viewportWidth=maxWidth
            val columns=if(!landscape)1 else when(s.page){ConjugateChoicePage.TODAY->3;ConjugateChoicePage.TENSE->3;else->2}
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).heightIn(min=height).padding(horizontal=20.dp,vertical=if(short)6.dp else 24.dp),
                horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                val changeTense=s.page==ConjugateChoicePage.TYPE && s.originalTenses.size>1
                Text(heading,Modifier.widthIn(max=1000.dp).fillMaxWidth()
                    .then(if(changeTense)Modifier.clickable(onClickLabel=stringResource(R.string.conj_tense),onClick=onTenses) else Modifier)
                    .padding(8.dp).testTag("conj_select_heading"),color=LearningBlue,fontSize=if(short)18.sp else 23.sp,lineHeight=if(short)22.sp else 29.sp,
                    fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
                Spacer(Modifier.height(if(short)6.dp else 24.dp))
                val measure=rememberTextMeasurer()
                val density=LocalDensity.current
                val labelStyle=LocalTextStyle.current.copy(fontSize=if(short && landscape)17.sp else 20.sp,lineHeight=if(short && landscape)21.sp else 25.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
                val contentWidth=minOf(viewportWidth-40.dp,if(landscape)1000.dp else 420.dp)
                Column(Modifier.widthIn(max=if(landscape)1000.dp else 420.dp).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(if(short)10.dp else 14.dp)) {
                    options.chunked(columns).forEach { row ->
                        val textWidth=(contentWidth-14.dp*(row.size-1))/row.size-32.dp
                        val rowHeight=maxOf(if(short)48.dp else 64.dp,with(density){row.maxOf {
                            measure.measure(it.text,labelStyle,constraints=Constraints(maxWidth=textWidth.roundToPx())).size.height
                        }.toDp()}+(if(short)14.dp else 25.dp))
                        Row(Modifier.fillMaxWidth().height(rowHeight),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                            row.forEach { option ->
                                PremiumGradientButton(option.action,Color(0xFF389664),SuccessGreen,Color.White.copy(alpha=.2f),
                                    Modifier.weight(1f).fillMaxHeight().heightIn(min=if(short)48.dp else 64.dp).testTag("conj_select_${option.id}"),
                                    contentPadding=PaddingValues(horizontal=16.dp,vertical=if(short)6.dp else 12.dp)) {
                                    Text(option.text,Modifier.fillMaxWidth().testTag("conj_label_${option.id}"),color=Color.White,fontSize=if(short && landscape)17.sp else 20.sp,lineHeight=if(short && landscape)21.sp else 25.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
