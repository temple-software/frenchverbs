package com.templesoftware.frenchverbs.ui.verbs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.templesoftware.frenchverbs.R
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.VerbState
import com.templesoftware.frenchverbs.ui.components.*
import com.templesoftware.frenchverbs.ui.navigation.NavigationAction
import com.templesoftware.frenchverbs.ui.theme.educationalColors

@Composable
fun SelectionScreen(section: String, state: VerbState, language: AppLanguage,
    onLanguage: (AppLanguage)->Unit, onToggle: (String?)->Unit, onNext: ()->Unit, onBack: ()->Unit, onAbout: ()->Unit) {
    if(section=="tenses") {
        TensesScreen(state,language,onLanguage,onToggle,onNext,onAbout)
        return
    }
    VerbSelectionScreen(section,state,language,onToggle,onNext,onBack)
}

@Composable
fun PrimaryAction(text:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true) {
    val colors=MaterialTheme.educationalColors
    PremiumGradientButton(onClick,colors.primaryGradientTop,colors.primaryGradientBottom,colors.glossyHighlight,
        modifier.heightIn(min=52.dp),enabled=enabled,tactilePress=true) {
        Text(text,color=Color.White,style=MaterialTheme.typography.labelLarge,textAlign=TextAlign.Center)
    }
}

@Composable
fun ActivityScreen(language:AppLanguage,onHome:()->Unit,onActivity:(String)->Unit) {
    TempleScreenScaffold(stringResource(R.string.activities_title),NavigationAction.HOME,onHome,language) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(20.dp),contentAlignment=Alignment.Center) {
            val columns=if(maxWidth>maxHeight && LocalDensity.current.fontScale<1.8f) 2 else 1
            LazyVerticalGrid(GridCells.Fixed(columns),Modifier.widthIn(max=760.dp).fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(8.dp)) {
                items(listOf("learn","repeat","conjugate","use")) { route ->
                    MaterialTheme(typography = MaterialTheme.typography.copy(
                        labelLarge = MaterialTheme.typography.labelLarge.copy(fontSize = 19.sp)
                    )) {
                    PrimaryAction(stringResource(activityTitle(route)),{onActivity(route)},Modifier.heightIn(min=76.dp).testTag("open_$route"))
                    }
                }
            }
        }
    }
}
private fun activityTitle(route:String)=when(route){"learn"->R.string.screen_learn;"repeat"->R.string.screen_repeat;"conjugate"->R.string.screen_conjugate;else->R.string.screen_use}

@Composable
fun PlaceholderScreen(route:String,language:AppLanguage,onHome:()->Unit) {
    TempleScreenScaffold(stringResource(activityTitle(route)),NavigationAction.HOME,onHome,language) {
        Column(Modifier.widthIn(max=560.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text(stringResource(R.string.coming_soon),style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
            Text(stringResource(when(route){"repeat"->R.string.repeat_notice;"conjugate"->R.string.conjugate_notice;else->R.string.use_notice}),textAlign=TextAlign.Center)
        }
    }
}

