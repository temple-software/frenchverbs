package com.templesoftware.frenchverbs
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.ui.app.UseViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
class UsePass38RotationTest {
    private val compose=createAndroidComposeRule<UsePreviewActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    @Test fun nativeRotationRetainsLiveEntryAndHints() {
        compose.mainClock.autoAdvance=false
        lateinit var m:UseViewModel
        compose.activityRule.scenario.onActivity{m=ViewModelProvider(it)[UseViewModel::class.java]}
        val out=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass38").apply{mkdirs()}
        val ready=File(out,"rotation-ready");ready.delete()
        fun rotate(step:String) {
            val state=m.state.value;val orientation=compose.activity.resources.configuration.orientation
            ready.writeText(step)
            val deadline=System.currentTimeMillis()+30000;var changed=false
            while(!changed&&System.currentTimeMillis()<deadline){Thread.sleep(100);compose.activityRule.scenario.onActivity{changed=it.resources.configuration.orientation!=orientation}}
            assertTrue("native rotation $step",changed);assertEquals(state,m.state.value)
        }
        lateinit var audio:com.templesoftware.frenchverbs.ui.use.UseAudioModel
        compose.activityRule.scenario.onActivity{audio=ViewModelProvider(it)[com.templesoftware.frenchverbs.ui.use.UseAudioModel::class.java]}
        compose.waitUntil(20000){audio.player.ready}
        val q=m.state.value.current!!
        fun verify() {
            compose.mainClock.advanceTimeByFrame()
            val entry=m.state.value.input.entries[q.person]
            compose.onNodeWithTag("use_live_sentence",true).assertTextEquals(q.prefix+com.templesoftware.frenchverbs.ui.use.useDisplayEntry(q.form,entry)+q.suffix)
            for(i in entry.indices)compose.onNodeWithTag("use_box_${i}_letter",true).assertTextEquals(entry[i].toString())
        }
        q.answer.take(3).forEach{compose.onNodeWithTag("quiz_key_$it").performClick()}
        verify();val played=audio.player.starts;val capacity=q.boxCount;rotate("typed");verify()
        compose.activityRule.scenario.onActivity{assertSame(audio,ViewModelProvider(it)[com.templesoftware.frenchverbs.ui.use.UseAudioModel::class.java])}
        assertEquals(played,audio.player.starts);assertEquals(capacity,m.state.value.current!!.boxCount)
        compose.onNodeWithTag("quiz_key_${q.answer.last()}").performClick();assertEquals(played+1,audio.player.starts)
        compose.runOnIdle {
            m.clear()
            repeat(2){m.letter(q.keys.first{it!=q.answer.first()});m.submit();m.tick(650)}
        }
        assertTrue(m.state.value.input.hints[q.person].isNotEmpty())
        verify();rotate("hint");verify()
        compose.runOnIdle{m.clear()};verify()
        assertEquals(m.state.value.input.hints[q.person],m.state.value.input.entries[q.person])
        ready.delete()
    }
}
