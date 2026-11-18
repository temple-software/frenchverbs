package com.templesoftware.frenchverbs.data.preferences

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class AppLanguageTest {
    @Test fun systemDefaults() {
        listOf(Locale.FRANCE, Locale.CANADA_FRENCH, Locale.forLanguageTag("fr-BE")).forEach {
            assertEquals(AppLanguage.FRENCH, AppLanguage.fromSystem(it))
        }
        listOf(Locale.UK, Locale.US, Locale.GERMANY, Locale.forLanguageTag("es-ES"), Locale.ITALY, Locale.ROOT).forEach {
            assertEquals(AppLanguage.ENGLISH_UK, AppLanguage.fromSystem(it))
        }
    }
    @Test fun storedLanguageTags() {
        assertEquals(AppLanguage.ENGLISH_UK, AppLanguage.fromLanguageTag("EN-gb"))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromLanguageTag("fr"))
        assertNull(AppLanguage.fromLanguageTag(null))
        assertNull(AppLanguage.fromLanguageTag("unknown"))
    }
}
