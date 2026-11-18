package com.templesoftware.frenchverbs.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreUserPreferencesRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun freshPlatformInstallFollowsDeviceWithoutCreatingAnOverride() = runBlocking {
        for (device in AppLanguage.entries) {
            val job = SupervisorJob()
            val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) {
                File(folder.root, "fresh-${device.name}.preferences_pb")
            }
            val platform = object : PlatformAppLanguage {
                override fun read(): AppLanguage? = null
                override fun write(language: AppLanguage) = error("A fresh install must follow the device")
            }
            try {
                val repository = DataStoreUserPreferencesRepository(store, device, platform)
                assertEquals(device, repository.preferences.first().language)
                assertFalse(store.data.first().asMap().keys.any { it.name == "language" })
            } finally { job.cancelAndJoin() }
        }
    }

    @Test fun platformMigrationSwitchingAndSystemResetPreserveOtherPreferences() = runBlocking {
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) {
            File(folder.root, "platform.preferences_pb")
        }
        var selected: AppLanguage? = null
        var writes = 0
        val platform = object : PlatformAppLanguage {
            override fun read() = selected
            override fun write(language: AppLanguage) { selected = language; writes++ }
        }
        try {
            store.edit { it[stringPreferencesKey("language")] = "fr" }
            val repository = DataStoreUserPreferencesRepository(store, AppLanguage.ENGLISH_UK, platform)
            assertEquals(AppLanguage.FRENCH, repository.preferences.first().language)
            assertEquals(AppLanguage.FRENCH, selected)
            assertEquals(1, writes)
            repository.preferences.first()
            assertEquals(1, writes)
            repository.setLanguage(AppLanguage.ENGLISH_UK)
            assertEquals(AppLanguage.ENGLISH_UK, selected)
            selected = AppLanguage.FRENCH // Android's Settings language picker.
            assertEquals(AppLanguage.FRENCH, repository.preferences.first().language)
            selected = null // Follow device language again.
            assertEquals(AppLanguage.ENGLISH_UK, repository.preferences.first().language)
        } finally { job.cancelAndJoin() }
    }


    @Test fun explicitChoiceAlwaysWinsAndAbsentPreferenceIsNeverWritten() = runBlocking {
        val tags = listOf("fr-FR", "fr-CA", "fr-BE", "en-GB", "en-US", "de-DE", "es-ES")
        for (tag in tags) {
            val file = File(folder.root, "$tag.preferences_pb")
            val job = SupervisorJob()
            val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
            try {
                val fallback = AppLanguage.fromSystem(java.util.Locale.forLanguageTag(tag))
                val repository = DataStoreUserPreferencesRepository(store, fallback)
                assertEquals(if (tag.startsWith("fr")) AppLanguage.FRENCH else AppLanguage.ENGLISH_UK,
                    repository.preferences.first().language)
                assertTrue(store.data.first().asMap().isEmpty())
                for (selection in AppLanguage.entries) {
                    repository.setLanguage(selection)
                    for (changedDeviceLanguage in AppLanguage.entries) {
                        assertEquals(selection, DataStoreUserPreferencesRepository(store, changedDeviceLanguage)
                            .preferences.first().language)
                    }
                }
                store.edit { it.clear() }
                assertEquals(fallback, repository.preferences.first().language)
                assertTrue(store.data.first().asMap().isEmpty())
            } finally { job.cancelAndJoin() }
            val reloadJob = SupervisorJob()
            val reloaded = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + reloadJob)) { file }
            try {
                val repository = DataStoreUserPreferencesRepository(reloaded, AppLanguage.ENGLISH_UK)
                repository.setLanguage(AppLanguage.FRENCH)
            } finally { reloadJob.cancelAndJoin() }
            val finalJob = SupervisorJob()
            val finalStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + finalJob)) { file }
            try {
                assertEquals(AppLanguage.FRENCH,
                    DataStoreUserPreferencesRepository(finalStore, AppLanguage.ENGLISH_UK).preferences.first().language)
            } finally { finalJob.cancelAndJoin() }
        }
    }

    @Test fun liveDeviceFallbackChangesWithoutWritingOrOverridingChoice() = runBlocking {
        val file = File(folder.root, "live.preferences_pb")
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
        val repository = DataStoreUserPreferencesRepository(store, AppLanguage.ENGLISH_UK)
        try {
            repository.updateSystemLanguage(AppLanguage.FRENCH)
            assertEquals(AppLanguage.FRENCH, repository.preferences.first().language)
            assertTrue(store.data.first().asMap().isEmpty())
            repository.setLanguage(AppLanguage.ENGLISH_UK)
            repository.updateSystemLanguage(AppLanguage.FRENCH)
            assertEquals(AppLanguage.ENGLISH_UK, repository.preferences.first().language)
        } finally { job.cancelAndJoin() }
    }

    @Test fun defaultsAndPersistenceAcrossStoreRecreation() = runBlocking {
        val file = File(folder.root, "language.preferences_pb")
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
        val repository = DataStoreUserPreferencesRepository(store, AppLanguage.FRENCH)
        try {
            assertEquals(UserPreferences(AppLanguage.FRENCH), repository.preferences.first())
            assertTrue(store.data.first().asMap().isEmpty())
            repository.setLanguage(AppLanguage.ENGLISH_UK)
            assertEquals(UserPreferences(AppLanguage.ENGLISH_UK), repository.preferences.first())
            assertEquals(setOf("language"), store.data.first().asMap().keys.map { it.name }.toSet())
        } finally { job.cancelAndJoin() }
        val restoredJob = SupervisorJob()
        val restored = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + restoredJob)) { file }
        try {
            val restoredRepository = DataStoreUserPreferencesRepository(restored, AppLanguage.FRENCH)
            assertEquals(UserPreferences(AppLanguage.ENGLISH_UK), restoredRepository.preferences.first())
            restoredRepository.setLanguage(AppLanguage.FRENCH)
            assertEquals(UserPreferences(AppLanguage.FRENCH), restoredRepository.preferences.first())
            restored.edit { it[stringPreferencesKey("language")] = "unsupported" }
            assertEquals(UserPreferences(AppLanguage.FRENCH), restoredRepository.preferences.first())
        } finally { restoredJob.cancelAndJoin() }
    }
}
