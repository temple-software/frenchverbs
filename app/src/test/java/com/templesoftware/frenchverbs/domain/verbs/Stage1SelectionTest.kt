package com.templesoftware.frenchverbs.domain.verbs

import org.junit.Assert.*
import org.junit.Test

/** Stage 1 only: no learning-sequence or dictionary-generation tests. */
class Stage1SelectionTest {
    @Test fun exactOptionsAndFirstUseDefaults() {
        assertEquals(listOf("present","imperfect","future","perfect","historic","pluperfect"),Choices.tenses)
        assertEquals(listOf("important","er","ir","irregular"),Choices.types)
        assertEquals(listOf("être","avoir","aller","faire","dire","venir","prendre","pouvoir","voir","vouloir"),Choices.important)
        assertEquals(listOf("standard","ger","cer","yer","e_grave","accent","double","special"),Choices.er)
        assertEquals(listOf("partir","mettre","connaitre","ecrire","lire","ouvrir","recevoir","conduire","peindre"),Choices.families)
        val s=Selection()
        assertEquals(setOf("present"),s.tenses);assertEquals(setOf("important"),s.types)
        assertEquals(Choices.important.take(4).toSet(),s.important)
        assertEquals(Choices.er.take(4).toSet(),s.er);assertEquals(Choices.families.take(3).toSet(),s.families)
    }
    @Test fun everyOptionSupportsIndependentMultipleAndEmptySelection() {
        for(section in listOf("tenses","types","important","er","families")) {
            var s=Selection(tenses=Choices.tenses.toSet()).with(section,emptySet())
            assertFalse(s.canContinue(section))
            for(id in s.visible(section)) {s=s.toggle(section,id);assertTrue(id in s.selected(section));assertTrue(s.canContinue(section))}
            val selected=s.selected(section).toList()
            for(id in selected)s=s.toggle(section,id)
            assertFalse(s.canContinue(section))
        }
    }
    @Test fun allTracksVisibleCategoriesAndPreservesHiddenSpecialFuture() {
        var s=Selection(tenses=setOf("present","future"),er=setOf("special"))
        assertEquals(8,s.visible("er").size)
        s=s.toggleAll("er");assertTrue(s.all("er"))
        s=s.toggle("er","cer");assertFalse(s.all("er"))
        s=s.toggle("er","cer");assertTrue(s.all("er"))
        s=s.toggle("tenses","future");assertEquals(7,s.visible("er").size);assertTrue(s.all("er"))
        s=s.toggleAll("er");assertEquals(setOf("special"),s.er);assertFalse(s.canContinue("er"))
        s=s.toggle("tenses","future");assertTrue(s.canContinue("er"));assertFalse(s.all("er"))
    }
    @Test fun invalidAndHiddenOptionsCannotBeToggled() {
        val s=Selection()
        for(section in listOf("tenses","types","important","er","families"))assertEquals(s,s.toggle(section,"invalid"))
        assertEquals(s,s.toggle("er","special"))
        assertEquals(setOf("present"),s.with("tenses",setOf("present","invalid")).tenses)
    }
    @Test fun noTypeCannotContinueAndRegularIrHasNoExtraSelectionScreen() {
        assertFalse(Selection(types=emptySet()).canContinue("types"))
        assertEquals(listOf("tenses","types","activities"),Selection(types=setOf("ir")).setupRoutes())
        assertEquals(listOf("tenses","types","er","families","activities"),Selection(types=setOf("er","irregular")).setupRoutes())
        assertNull(Selection().next("activities"))
    }
}
