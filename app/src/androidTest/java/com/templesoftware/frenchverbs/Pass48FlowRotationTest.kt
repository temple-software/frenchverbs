package com.templesoftware.frenchverbs
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.DataStoreSelectionRepository
import com.templesoftware.frenchverbs.domain.verbs.Selection
import com.templesoftware.frenchverbs.ui.app.VerbViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.*
import java.io.File

class Pass48FlowRotationTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    private val seed=object:ExternalResource(){override fun before(){
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".audit43")){"Isolated audit installation required"}
        runBlocking{DataStoreSelectionRepository(context).complete(Selection(tenses=setOf("present","future"), types=setOf("er")))}
    }}
    @get:Rule val rules=RuleChain.outerRule(seed).around(PreserveRotationRule()).around(compose)
    @Test fun selectionNavigationPersistenceAndNativeRotation() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val out=File(context.getExternalFilesDir(null),"pass48flow").apply{mkdirs()}
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
        compose.onNodeWithTag("next").performClick();visible("choice_er");rotate("types","choice_er")
        compose.onNodeWithTag("next").performClick();visible("choice_standard");rotate("er","choice_standard")
        compose.onNodeWithTag("choice_special").performScrollTo().performClick().assertIsOn()
        compose.activityRule.scenario.onActivity{it.onBackPressedDispatcher.onBackPressed()};visible("choice_er")
        compose.onNodeWithTag("next").performClick();visible("choice_special")
        compose.onNodeWithTag("choice_special").assertIsOn()
        compose.onNodeWithTag("next").performClick();visible("open_learn")
        compose.activityRule.scenario.recreate();visible("open_learn")
        val repo=DataStoreSelectionRepository(context)
        val completed=runBlocking{repo.completedSelections.first()}
        assertNotNull(completed);assertTrue("special" in completed!!.er)
        File(out,"result.txt").writeText("PASS Welcome, tenses, types, Regular -er: native rotation both ways, recreation/background; back/forward retains special category; completed selection persisted.")
    }
}
