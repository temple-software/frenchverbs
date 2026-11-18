package com.templesoftware.frenchverbs.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.templesoftware.frenchverbs.data.preferences.*
import com.templesoftware.frenchverbs.ui.app.UseViewModel
import com.templesoftware.frenchverbs.ui.use.*
import com.templesoftware.frenchverbs.ui.app.VerbViewModel
import com.templesoftware.frenchverbs.ui.app.RepeatViewModel
import com.templesoftware.frenchverbs.ui.app.ConjugateViewModel
import com.templesoftware.frenchverbs.ui.quiz.*
import com.templesoftware.frenchverbs.ui.verbs.*
import com.templesoftware.frenchverbs.ui.about.AboutScreen

@Composable
fun AppNavHost(navController:NavHostController,language:AppLanguage,onLanguageSelected:(AppLanguage)->Unit,selectionModel:VerbViewModel?=null) {
    val context=LocalContext.current.applicationContext
    val model:VerbViewModel=selectionModel ?: viewModel(factory=remember(context) { viewModelFactory {
        initializer { VerbViewModel(DataStoreSelectionRepository(context),createSavedStateHandle()) }
    } })
    val repeatModel:RepeatViewModel=viewModel(factory=remember { viewModelFactory {
        initializer { RepeatViewModel(createSavedStateHandle()) }
    } })
    val quizModel:ConjugateViewModel=viewModel(factory=remember { viewModelFactory {
        initializer { ConjugateViewModel(createSavedStateHandle()) }
    } })
    val useAudio=rememberUseAudio()
    val useModel:UseViewModel=viewModel(factory=remember { viewModelFactory { initializer { UseViewModel(createSavedStateHandle()) } } })
    val state by model.state.collectAsStateWithLifecycle()
    if(!state.loaded) return
    fun home() { navController.popBackStack("activities",false) }
    val initialRoute=rememberSaveable { if(state.completedSelection!=null)"welcome"else "tenses" }
    fun editSelection() {
        if(!navController.popBackStack("tenses",false))navController.navigate("tenses") {
            popUpTo("activities"){inclusive=true};launchSingleTop=true
        }
    }
    NavHost(navController,startDestination=initialRoute) {
        composable("welcome") {
            state.completedSelection?.let { selection ->
                WelcomeScreen(selection,language,{
                    model.useCompletedSelection()
                    navController.navigate("activities"){launchSingleTop=true}
                },{
                    model.useCompletedSelection()
                    navController.navigate("tenses"){launchSingleTop=true}
                })
            }
        }
        listOf("tenses","types","important","er","families").forEach { section ->
            composable(section) {
                BackHandler(enabled=state.savingSelection) {}
                SelectionScreen(section,state,language,onLanguageSelected,{model.toggle(section,it)},
                    { if(state.selection.canContinue(section)) state.selection.next(section)?.let { route ->
                        if(route=="activities")model.completeSelection { navController.navigate(route){launchSingleTop=true} }
                        else navController.navigate(route){launchSingleTop=true}
                    } },
                    {if(!state.savingSelection)navController.popBackStack()},{navController.navigate("about") { launchSingleTop=true }})
            }
        }
        composable("about") { AboutScreen(language,{navController.popBackStack()}) }
        composable("activities") {
            ActivityScreen(language,::editSelection) { route ->
                val ready=when(route){"learn"->model.startLearn();"repeat"->repeatModel.startFull(state.selection);"conjugate"->quizModel.start(state.selection);"use"->useModel.start(state.selection);else->true}
                if(ready) navController.navigate(route) {launchSingleTop=true}
            }
        }
        composable("learn") {
            BackHandler { home() }
            LearnScreen(state,language,::home,onPrevious={model.previous()},onNext={
                if(!model.advance() && repeatModel.startFull(state.selection)) navController.navigate("repeat") { popUpTo("activities"); launchSingleTop=true }
            },onRepeat={state.current?.let { repeatModel.startSingle(it);navController.navigate("repeat"){launchSingleTop=true} }})
        }
        composable("repeat") { entry ->
            val repeatState by repeatModel.state.collectAsStateWithLifecycle()
            fun exitRepeat(toLearn:Boolean) {
                repeatModel.cancel()
                if(toLearn) navController.popBackStack("learn",false) else home()
            }
            BackHandler { exitRepeat(repeatState.single) }
            LaunchedEffect(repeatState.complete,repeatState.single) {
                if(repeatModel.state.value.complete && repeatModel.state.value.single && navController.currentBackStackEntry==entry) navController.popBackStack("learn",false)
            }
            RepeatClock(repeatModel)
            RepeatScreen(repeatState,repeatModel.timeline,language,{exitRepeat(false)},{
                if(quizModel.start(state.selection)) {
                    repeatModel.cancel()
                    navController.navigate("conjugate"){popUpTo("activities");launchSingleTop=true}
                }
            },onNext={repeatModel.next()})
        }
        composable("conjugate") {
            val quizState by quizModel.state.collectAsStateWithLifecycle()
            fun leaveQuiz(){quizModel.cancel();home()}
            val choices by quizModel.selection.collectAsStateWithLifecycle()
            fun back(){if(!quizModel.selectionBack())leaveQuiz()}
            BackHandler { if(choices.done)leaveQuiz() else back() }
            if(!choices.done) {
                ConjugateSelectionScreen(choices,language,::leaveQuiz,::back,
                    quizModel::chooseTenses,quizModel::chooseTypes,quizModel::chooseRandom,
                    quizModel::chooseTense,quizModel::chooseCategory)
            } else {
                ConjugateActivityContent(quizModel,language,::leaveQuiz)
            }
        }
        composable("use") {
            val useState by useModel.state.collectAsStateWithLifecycle()
            fun leaveUse(){useModel.cancel();home()}
            BackHandler { leaveUse() }
            val click=rememberUseKeyClick()
            UseFeedbackClock(useModel)
            UseScreen(useState,language,::leaveUse,{useModel.letterWithClick(it,click)},useModel::clear,useModel::submit,useModel::edit,onPause=useModel::pause,letterReady=useAudio.ready)
        }
    }
}
