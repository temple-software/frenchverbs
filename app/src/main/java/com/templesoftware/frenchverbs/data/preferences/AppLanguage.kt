package com.templesoftware.frenchverbs.data.preferences

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.templesoftware.frenchverbs.R
import java.util.Locale

/** Add a language here, its resources and locales_config.xml; consumers need no language branches. */
enum class AppLanguage(val languageTag: String, @param:StringRes val nameResource: Int,
    @param:DrawableRes val flagResource: Int, @param:DrawableRes val logoResource: Int) {
    ENGLISH_UK("en-GB", R.string.language_english, R.drawable.flag_uk, R.drawable.nice_logo_en),
    FRENCH("fr", R.string.language_french, R.drawable.flag_fr, R.drawable.nice_logo_fr);

    companion object {
        val supported: List<AppLanguage> = entries.toList()
        val fallback: AppLanguage = ENGLISH_UK
        fun fromLanguageTag(tag: String?): AppLanguage? =
            supported.firstOrNull { it.languageTag.equals(tag, ignoreCase = true) }
        fun fromSystem(locale: Locale = Locale.getDefault()): AppLanguage =
            supported.firstOrNull { Locale.forLanguageTag(it.languageTag).language == locale.language } ?: fallback
    }
}
