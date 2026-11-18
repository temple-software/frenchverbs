package com.templesoftware.frenchverbs

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.*
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.ui.app.ConjugateViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ConjugateSelectionRotationTest {
    private val compose=createAndroidComposeRule<ConjugateSelectionPreviewActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    @Test fun nativeRotationAndRestorationKeepOriginalFiltersAndAcceptedSubset() {
        val out=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass33").apply{mkdirs()}
        val ready=File(out,"rotation-ready");ready.delete()
        lateinit var model:ConjugateViewModel
        compose.activityRule.scenario.onActivity{model=ViewModelProvider(it)[ConjugateViewModel::class.java]}
        val original=model.selection.value.original
        fun rotate(step:String) {
            val selection=model.selection.value;val quiz=model.state.value
            val orientation=compose.activity.resources.configuration.orientation
            ready.writeText(step)
            val deadline=System.currentTimeMillis()+30000
            var changed=false
            while(!changed && System.currentTimeMillis()<deadline) {
                Thread.sleep(100)
                compose.activityRule.scenario.onActivity{changed=it.resources.configuration.orientation!=orientation}
            }
            assertTrue("native rotation $step",changed)
            assertEquals(selection,model.selection.value);assertEquals(quiz,model.state.value)
            assertEquals(original,model.selection.value.original)
        }
        compose.onNodeWithTag("conj_select_title").assertExists();rotate("today")
        compose.onNodeWithTag("conj_select_tense").performClick();compose.onNodeWithTag("conj_select_tense_0").performClick()
        assertEquals(0,model.selection.value.tense);rotate("type")
        val selected=model.selection.value
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED);compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity{model=ViewModelProvider(it)[ConjugateViewModel::class.java]}
        assertEquals(selected,model.selection.value)
        compose.onNodeWithTag("conj_select_type_important").performClick()
        val accepted=model.state.value.session
        compose.runOnIdle{model.letter(model.question!!.rows[0].answer.first())}
        rotate("quiz");rotate("return")
        assertEquals(accepted,model.state.value.session);assertEquals(accepted,model.selection.value.accepted)
        assertTrue(accepted.size in 1..6);assertTrue(model.state.value.entries[0].isNotEmpty())
        File(out,"rotation.txt").writeText("PASS four native rotations: Today, filtered Verb type, accepted quiz and return. Original source, filters, accepted subset and typed answer stable; background/resume and recreate stable.")
        ready.delete()
    }
}
