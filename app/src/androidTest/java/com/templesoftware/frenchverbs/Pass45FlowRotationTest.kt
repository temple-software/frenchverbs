package com.templesoftware.frenchverbs
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.DataStoreSelectionRepository
import com.templesoftware.frenchverbs.domain.verbs.Selection
import com.templesoftware.frenchverbs.ui.app.VerbViewModel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.*
import java.io.File

class Pass45FlowRotationTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    private val seed=object:ExternalResource(){override fun before(){
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".audit43")){"Isolated audit installation required"}
        runBlocking{DataStoreSelectionRepository(context).complete(Selection())}
    }}
    @get:Rule val rules=RuleChain.outerRule(seed).around(PreserveRotationRule()).around(compose)
    @Test fun welcomeSelectionAboutActivitiesLearnRotateAndRecreate() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val out=File(context.getExternalFilesDir(null),"pass45flow").apply{mkdirs()}
        val ready=File(out,"rotation-ready");ready.delete()
        fun visible(tag:String){compose.waitUntil(15000){compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()}}
        fun rotate(screen:String,tag:String) {
            for(step in 0..1) {
                lateinit var model:VerbViewModel
                compose.activityRule.scenario.onActivity{model=ViewModelProvider(it)[VerbViewModel::class.java]}
                val before=model.state.value
                val orientation=compose.activity.resources.configuration.orientation
                ready.writeText("$screen-$step")
                val deadline=System.currentTimeMillis()+30000;var changed=false
                while(!changed&&System.currentTimeMillis()<deadline){Thread.sleep(100);compose.activityRule.scenario.onActivity{changed=it.resources.configuration.orientation!=orientation}}
                assertTrue("$screen native rotation",changed);visible(tag)
                assertEquals("$screen retains session, selection and progress",before,model.state.value)
            }
            ready.delete()
            compose.activityRule.scenario.recreate();visible(tag)
            compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);visible(tag)
        }
        visible("welcome_again");rotate("welcome","welcome_again")
        compose.onNodeWithTag("welcome_change").performClick();visible("choice_present")
        compose.onNodeWithTag("choice_imperfect").performScrollTo().performClick();rotate("selection","choice_present")
        compose.onNodeWithTag("open_about").performClick();visible("about_title");rotate("about","about_title")
        compose.activityRule.scenario.onActivity{it.onBackPressedDispatcher.onBackPressed()};visible("next")
        repeat(6){if(compose.onAllNodesWithTag("open_learn").fetchSemanticsNodes().isEmpty())compose.onNodeWithTag("next").performClick()}
        visible("open_learn");rotate("activities","open_learn")
        compose.onNodeWithTag("open_learn").performClick();visible("learn_home");rotate("learn","learn_home")
        File(out,"result.txt").writeText("PASS Welcome, selection editing, About, Activities and Learn: native portrait/landscape/portrait, recreation and background/resume.")
    }
}
