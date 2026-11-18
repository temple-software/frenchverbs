package com.templesoftware.frenchverbs.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.templesoftware.frenchverbs.domain.verbs.Selection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import com.templesoftware.frenchverbs.domain.verbs.readyForActivities

private val Context.selectionStore by preferencesDataStore(name = "verb_selections")
interface SelectionRepository {
    val selections: Flow<Selection>
    val completedSelections: Flow<Selection?> get() = flowOf(null)
    suspend fun complete(selection: Selection) { save(selection) }
    suspend fun save(selection: Selection)
}
class DataStoreSelectionRepository internal constructor(private val store: DataStore<Preferences>) : SelectionRepository {
    constructor(context: Context) : this(context.applicationContext.selectionStore)
    override val selections = store.data.map { values ->
        sections.fold(Selection()) { s, section -> values[stringSetPreferencesKey(section)]?.let { s.with(section,it) } ?: s }
    }
    override val completedSelections = store.data.map { values ->
        if (values[booleanPreferencesKey("completed_selection")] != true ||
            sections.any { values[stringSetPreferencesKey("completed_$it")] == null }) null
        else sections.fold(Selection()) { s, section ->
            s.with(section, values[stringSetPreferencesKey("completed_$section")]!!)
        }.takeIf { it.readyForActivities() }
    }
    override suspend fun complete(selection: Selection) {
        require(selection.readyForActivities())
        store.edit { values ->
            sections.forEach {
                values[stringSetPreferencesKey(it)] = selection.selected(it)
                values[stringSetPreferencesKey("completed_$it")] = selection.selected(it)
            }
            values[booleanPreferencesKey("completed_selection")] = true
        }
    }
    override suspend fun save(selection: Selection) {
        store.edit { values -> sections.forEach { values[stringSetPreferencesKey(it)] = selection.selected(it) } }
    }
    companion object { val sections = listOf("tenses", "types", "important", "er", "families") }
}
