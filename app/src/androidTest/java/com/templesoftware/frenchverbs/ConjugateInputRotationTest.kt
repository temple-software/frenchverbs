package com.templesoftware.frenchverbs

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.ui.app.ConjugateViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ConjugateInputRotationTest {
    private val compose=createAndroidComposeRule<ConjugateInputPreviewActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    @Test fun rotateEditedWordThenContinueAndValidate() {
        val out=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass34").apply{mkdirs()}
        val ready=File(out,"rotation-ready");ready.delete()
        lateinit var model:ConjugateViewModel
        compose.activityRule.scenario.onActivity{model=ViewModelProvider(it)[ConjugateViewModel::class.java]}
        compose.onNodeWithTag("quiz_box_0_3",true).performTouchInput{click()}
        assertEquals("pou",model.state.value.entries[0])
        fun rotate(step:String) {
            val state=model.state.value;val orientation=compose.activity.resources.configuration.orientation
            ready.writeText(step)
            val deadline=System.currentTimeMillis()+30000;var changed=false
            while(!changed && System.currentTimeMillis()<deadline) {
                Thread.sleep(100);compose.activityRule.scenario.onActivity{changed=it.resources.configuration.orientation!=orientation}
            }
            assertTrue("native rotation $step",changed);assertEquals(state,model.state.value)
        }
        rotate("edited");rotate("return")
        "vais".forEach{compose.onNodeWithTag("quiz_key_$it").performClick()}
        assertEquals("pouvais",model.state.value.entries[0]);compose.onNodeWithTag("quiz_enter").performClick()
        assertTrue(0 in model.state.value.locked);assertEquals(List(6){0},model.state.value.mistakes)
        File(out,"rotation.txt").writeText("PASS native rotation in both directions after tapping v: pou preserved; entered vais after return; embedded Enter validated pouvoir imparfait correctly without an editing error.")
        ready.delete()
    }
}
