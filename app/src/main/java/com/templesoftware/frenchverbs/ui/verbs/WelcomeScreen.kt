package com.templesoftware.frenchverbs.ui.verbs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.templesoftware.frenchverbs.R
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.components.*
import com.templesoftware.frenchverbs.ui.navigation.NavigationAction
import com.templesoftware.frenchverbs.ui.theme.educationalColors
import com.templesoftware.frenchverbs.ui.theme.SuccessGreen

@Composable fun WelcomeScreen(selection:Selection,language:AppLanguage,onAgain:()->Unit,onChange:()->Unit) {
    val colors=MaterialTheme.educationalColors
    TempleScreenScaffold(stringResource(R.string.welcome_title),NavigationAction.NONE,{},language) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val sideBySide=maxWidth>maxHeight && maxWidth>=480.dp && LocalDensity.current.fontScale<1.5f
            val compact=if(sideBySide)maxHeight<400.dp else maxHeight<700.dp
            val short=maxHeight<260.dp || (maxHeight<700.dp && LocalDensity.current.fontScale in 1.01f..1.5f)
            val topGap=if(compact)0.dp else minOf(56.dp,maxHeight*.08f)
            @Composable fun Again(modifier:Modifier) {
                PremiumGradientButton(onAgain,Color(0xFF389664),SuccessGreen,Color.White.copy(alpha=.2f),
                    modifier.testTag("welcome_again"),tactilePress=true,contentPadding=PaddingValues(if(short)8.dp else if(compact)12.dp else 20.dp)) {
                    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(if(short)2.dp else if(compact)6.dp else 10.dp)) {
                        Text(stringResource(R.string.welcome_again),color=Color.White,fontSize=24.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
                        Text(if(compact && selection.tenses.size>3)stringResource(R.string.conj_tense)+": "+stringResource(R.string.welcome_selected,selection.tenses.size,Choices.tenses.size) else Choices.tenses.filter{it in selection.tenses}.map{stringResource(ChoiceResources.tense.getValue(it))}.joinToString(" · "),
                            Modifier.fillMaxWidth().testTag("welcome_tenses"),color=Color.White,fontSize=if(compact)16.sp else 17.sp,lineHeight=if(compact)20.sp else 23.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
                        Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(if(short)2.dp else 5.dp)) {
                            welcomeCategories(selection).forEach { category ->
                                val title=stringResource(when(category.category){"important"->R.string.important_title;"er"->R.string.er_title;"ir"->R.string.ir_title;else->R.string.families_title})
                                val names=category.selected.map { id -> when(category.section){"er"->stringResource(ChoiceResources.category.getValue(id));"families"->stringResource(ChoiceResources.family.getValue(id));else->id} }
                                val summary=if(category.section==null)title else title+": "+if(category.showNames && !compact)names.joinToString(", ") else stringResource(R.string.welcome_selected,category.selected.size,category.total)
                                Text(summary,Modifier.fillMaxWidth().testTag("welcome_category_${category.category}"),color=Color.White,fontSize=if(short)15.sp else 16.sp,lineHeight=if(short)20.sp else 22.sp,textAlign=TextAlign.Center)
                            }
                        }
                    }
                }
            }
            @Composable fun Change(modifier:Modifier) {
                PremiumGradientButton(onChange,colors.primaryGradientTop,colors.primaryGradientBottom,colors.glossyHighlight,
                    modifier.heightIn(min=76.dp).testTag("welcome_change"),tactilePress=true,contentPadding=PaddingValues(if(short)8.dp else if(compact)12.dp else 20.dp)) {
                    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(if(short)2.dp else if(compact)6.dp else 10.dp)) {
                        Text(stringResource(R.string.welcome_change),color=Color.White,fontSize=24.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
                        Text(stringResource(R.string.welcome_change_description),Modifier.fillMaxWidth().testTag("welcome_change_description"),color=Color.White,fontSize=if(short)15.sp else 16.sp,lineHeight=if(short)20.sp else 22.sp,textAlign=TextAlign.Center)
                    }
                }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if(short)10.dp else if(compact)12.dp else 24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=if(sideBySide)Arrangement.Center else Arrangement.Top) {
                if(!sideBySide)Spacer(Modifier.height(topGap))
                Layout(
                    content={Again(Modifier.fillMaxWidth());Change(Modifier.fillMaxWidth())},
                    modifier=Modifier.widthIn(max=if(sideBySide)1000.dp else 600.dp).fillMaxWidth(),
                ) { measurables,constraints ->
                    val gap=20.dp.roundToPx()
                    val buttonWidth=if(sideBySide)(constraints.maxWidth-gap)/2 else constraints.maxWidth
                    // Measure both at the same width, then give both the taller content's height.
                    val buttonHeight=measurables.maxOf { it.maxIntrinsicHeight(buttonWidth) }
                    val buttons=measurables.map { it.measure(Constraints.fixed(buttonWidth,buttonHeight)) }
                    layout(constraints.maxWidth,if(sideBySide)buttonHeight else buttonHeight*2+gap) {
                        buttons[0].placeRelative(0,0)
                        buttons[1].placeRelative(if(sideBySide)buttonWidth+gap else 0,if(sideBySide)0 else buttonHeight+gap)
                    }
                }
            }
        }
    }
}

