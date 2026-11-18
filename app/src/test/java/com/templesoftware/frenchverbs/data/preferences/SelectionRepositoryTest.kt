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

class SelectionRepositoryTest {
    @get:Rule val folder=TemporaryFolder()
    @Test fun defaultsChangesEmptyChoicesAndHiddenCategorySurviveDiskReopen()=runBlocking {
        val file=File(folder.root,"selections.preferences_pb")
        var job=SupervisorJob()
        fun store()=PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+job)){file}
        var repo=DataStoreSelectionRepository(store())
        assertEquals(Selection(),repo.selections.first())
        val changed=Selection(tenses=emptySet(),important=emptySet(),er=setOf("special"),families=setOf("peindre"),types=setOf("er","irregular"))
        repo.save(changed); assertEquals(changed,repo.selections.first()); job.cancelAndJoin()
        job=SupervisorJob(); repo=DataStoreSelectionRepository(store())
        assertEquals(changed,repo.selections.first()); job.cancelAndJoin()
    }
}
