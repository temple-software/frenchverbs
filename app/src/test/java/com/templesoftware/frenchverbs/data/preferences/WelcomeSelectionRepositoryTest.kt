package com.templesoftware.frenchverbs.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.templesoftware.frenchverbs.domain.verbs.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

class WelcomeSelectionRepositoryTest {
    @get:Rule val folder=TemporaryFolder()
    @Test fun onlyCompletedSelectionsSurviveReopenAsWelcomeCandidates()=runBlocking {
        val file=File(folder.root,"welcome.preferences_pb")
        var job=SupervisorJob()
        fun repository()=DataStoreSelectionRepository(PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+job)){file})
        var repo=repository()
        assertNull(repo.completedSelections.first())
        val legacy=Selection(important=setOf("être","avoir"))
        repo.save(legacy);assertEquals(legacy,repo.selections.first());assertNull(repo.completedSelections.first())
        repo.complete(legacy)
        val another=legacy.copy(tenses=setOf("present","future"))
        repo.save(another) // Legacy/draft storage cannot replace the completed snapshot.
        assertEquals(legacy,repo.completedSelections.first())
        job.cancelAndJoin();job=SupervisorJob();repo=repository()
        assertEquals(legacy,repo.completedSelections.first())
        repo.complete(another);assertEquals(another,repo.completedSelections.first())
        job.cancelAndJoin()
    }
    @Test fun incompleteSelectionsCannotBeCommitted()=runBlocking {
        val job=SupervisorJob()
        val repo=DataStoreSelectionRepository(PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+job)){File(folder.root,"empty.preferences_pb")})
        for(s in listOf(Selection(tenses=emptySet()),Selection(types=emptySet()),Selection(important=emptySet()),Selection(types=setOf("er"),er=setOf("special")))) {
            assertFalse(s.readyForActivities())
            try{repo.complete(s);fail("incomplete selection was committed")}catch(_:IllegalArgumentException){}
        }
        assertNull(repo.completedSelections.first());job.cancelAndJoin()
    }
    @Test fun compactSummaryRespectsVisibleChoicesAndThreeOptionRule() {
        val s=Selection(types=Choices.types.toSet(),important=setOf("être","avoir","aller"),er=Choices.er.toSet(),families=setOf("partir"))
        val categories=welcomeCategories(s)
        assertEquals(Choices.types,categories.map{it.category})
        assertTrue(categories[0].showNames);assertEquals(listOf("être","avoir","aller"),categories[0].selected)
        assertFalse(categories[1].showNames);assertEquals(7,categories[1].total);assertFalse("special" in categories[1].selected)
        assertNull(categories[2].section)
        assertTrue(categories[3].showNames)
        assertEquals(8,welcomeCategories(s.copy(tenses=setOf("future")))[1].total)
        assertFalse(welcomeCategories(s.copy(important=Choices.important.take(4).toSet()))[0].showNames)
        assertTrue(WelcomeCategory("example","example",listOf("a","b","c"),3).showNames)
    }
}
