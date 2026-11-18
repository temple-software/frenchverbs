package com.templesoftware.frenchverbs

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.*
import com.templesoftware.frenchverbs.domain.repeat.*
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import com.templesoftware.frenchverbs.ui.verbs.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random
import java.io.File

class RepeatStage4RegressionTest {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    class CountedRandom(seed:Int):Random() {
        private val delegate=Random(seed)
        var calls=0
        override fun nextBits(bitCount:Int):Int {calls++;return delegate.nextBits(bitCount)}
    }
    @Test fun selectedSequenceIsBuiltOnceAndNextNeverRebuilds() {
        compose.runOnIdle {
            val dictionary=OfflineDictionary.verbs.associateBy{it.id}
            val cases=listOf(
                Selection(Choices.tenses.toSet(),Choices.types.toSet(),Choices.important.toSet(),Choices.er.toSet(),Choices.families.toSet()),
                Selection(setOf("present","perfect","pluperfect"),setOf("important","er","irregular"),setOf("avoir","aller"),setOf("ger","yer","special"),setOf("recevoir","partir")),
                Selection(setOf("future"),setOf("er","ir"),emptySet(),setOf("special"),emptySet()),
                Selection(setOf("historic","imperfect"),setOf("irregular"),emptySet(),emptySet(),setOf("mettre","ecrire","peindre")))
            cases.forEachIndexed { seed,selection ->
                val random=CountedRandom(seed);val saved=SavedStateHandle();val m=RepeatViewModel(saved)
                assertTrue(m.startFull(selection,random))
                val session=m.state.value.session;val calls=random.calls
                assertEquals(LearningSequence.create(selection,random=Random(seed)),session)
                val tenses=Choices.tenses.indices.filter{Choices.tenses[it] in selection.tenses}
                val keys=buildList {
                    if("important" in selection.types)addAll(Choices.important.filter{it in selection.important}.map{"id:$it"})
                    if("er" in selection.types)addAll(selection.visible("er").filter{it in selection.er}.map{"group:$it"})
                    if("ir" in selection.types)add("group:ir")
                    if("irregular" in selection.types)addAll(Choices.families.filter{it in selection.families}.map{"group:$it"})
                }
                val groups=session.chunked(tenses.size)
                assertEquals(keys.size,groups.size)
                groups.zip(keys).forEach{(items,key)->
                    assertEquals(tenses,items.map{it.tense});assertEquals(1,items.map{it.verbId}.distinct().size)
                    val id=items.first().verbId
                    assertEquals(key,if(key.startsWith("id:"))"id:$id" else "group:${dictionary.getValue(id).group}")
                }
                session.indices.forEach { index ->
                    assertEquals(session[index],m.state.value.current)
                    m.tick(m.timeline.waitingAt-1);assertEquals(RepeatPlayback.PLAYING,m.state.value.playback)
                    m.tick(1);assertEquals(RepeatPlayback.WAITING,m.state.value.playback)
                    val waiting=m.state.value;m.tick(Long.MAX_VALUE);assertEquals(waiting,m.state.value)
                    assertEquals(waiting,RepeatViewModel(saved).state.value)
                    if(index<session.lastIndex) {
                        assertTrue(m.next());repeat(4){assertFalse(m.next())}
                        m.tick(m.timeline.timing.finalFadeOut-1);assertEquals(index,m.state.value.index)
                        m.tick(1);assertEquals(index+1,m.state.value.index)
                    } else assertFalse(m.next())
                    assertSame(session,m.state.value.session);assertEquals(calls,random.calls)
                }
                assertTrue(m.startFull(selection,Random(seed+30)))
                val next=m.state.value.session
                assertEquals(LearningSequence.create(selection,session,Random(seed+30)),next)
                next.chunked(tenses.size).filter{dictionary.getValue(it.first().verbId).group!="important"}.forEach { group ->
                    val v=dictionary.getValue(group.first().verbId)
                    if(OfflineDictionary.verbs.any{it.group==v.group && session.none{old->old.verbId==it.id}})
                        assertFalse(session.any{it.verbId==v.id})
                }
            }
        }
    }
    @Test fun renderedDictionaryFormsAndSubjectFirstOrderAcrossAllTenses() {
        val timeline=RepeatTimeline()
        val item=mutableStateOf(RepeatState(listOf(LearningItem("aller",0,0))))
        compose.activityRule.scenario.onActivity{it.setContent{
            LocalizedContent(AppLanguage.ENGLISH_UK){TempleTheme{RepeatScreen(item.value,timeline,AppLanguage.ENGLISH_UK,{},{})}}
        }}
        val ids=(listOf("être","avoir","aller","venir")+
            (Choices.er+listOf("ir")+Choices.families).map{group->OfflineDictionary.verbs.first{it.group==group}.id}).distinct()
        var rowsChecked=0
        for(id in ids)for(tense in 0..5) {
            val forms=OfflineDictionary.verbs.first{it.id==id}.tenses[tense].forms
            assertEquals(6,forms.size)
            compose.runOnIdle{item.value=RepeatState(listOf(LearningItem(id,tense,0)),elapsed=timeline.waitingAt,playback=RepeatPlayback.WAITING)}
            for(row in 0..5) {
                compose.onNodeWithTag("repeat_row_$row",true).assertTextEquals(forms[row].text)
                rowsChecked++
            }
            // Sample every subject-only reveal on each dictionary item; completed earlier rows remain exact.
            for(row in 0..5) {
                val phase=timeline.phases.first{it.name=="subjectHold" && it.row==row}
                compose.runOnIdle{item.value=item.value.copy(elapsed=phase.start+phase.duration/2,playback=RepeatPlayback.PLAYING)}
                compose.onNodeWithTag("repeat_row_$row",true).assertTextEquals(forms[row].subject.trimEnd())
                if(row>0)compose.onNodeWithTag("repeat_row_${row-1}",true).assertTextEquals(forms[row-1].text)
                if(row<5)compose.onNodeWithTag("repeat_row_${row+1}",true).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
            }
        }
        val out=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass30").apply{mkdirs()}
        File(out,"dictionary.txt").writeText("PASS ${ids.size} verbs x 6 tenses x 6 persons = $rowsChecked exact dictionary forms, plus $rowsChecked subject-only reveal states. Verbs: $ids")
    }
    @Test fun finalActionBothLanguagesAndSingleCompletionInterval() {
        val language=mutableStateOf(AppLanguage.ENGLISH_UK)
        val t=RepeatTimeline();val state=mutableStateOf(RepeatState(listOf(LearningItem("aller",3,0)),elapsed=t.waitingAt,playback=RepeatPlayback.WAITING))
        var tries=0
        compose.activityRule.scenario.onActivity{it.setContent{
            LocalizedContent(language.value){TempleTheme{RepeatScreen(state.value,t,language.value,{},{tries++})}}
        }}
        for(lang in AppLanguage.entries) {
            compose.runOnIdle{language.value=lang}
            compose.onNodeWithTag("repeat_try").assertTextEquals(if(lang==AppLanguage.FRENCH)"À ton tour !" else "Try yourself").performClick()
        }
        assertEquals(2,tries)
        compose.runOnIdle {
            val m=RepeatViewModel(SavedStateHandle());m.startSingle(LearningItem("aller",3,1))
            m.tick(t.waitingAt);assertFalse(m.state.value.complete)
            m.tick(t.timing.completedHold);assertFalse(m.state.value.complete)
            assertEquals(t.fadeOutAt,m.state.value.elapsed)
            m.tick(t.timing.finalFadeOut-1);assertFalse(m.state.value.complete)
            m.tick(1);assertTrue(m.state.value.complete)
            val done=m.state.value;m.tick(999999);assertEquals(done,m.state.value)
        }
    }
}
