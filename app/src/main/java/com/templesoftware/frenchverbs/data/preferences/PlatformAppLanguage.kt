package com.templesoftware.frenchverbs.data.preferences

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

/** Android owns the per-app locale on API 33+. Older devices retain our DataStore/localized context. */
internal interface PlatformAppLanguage {
    fun read(): AppLanguage?
    fun write(language: AppLanguage)

    companion object {
        fun from(context: Context): PlatformAppLanguage? {
            if (Build.VERSION.SDK_INT < 33) return null
            val manager = context.applicationContext.getSystemService(LocaleManager::class.java)
            return object : PlatformAppLanguage {
                override fun read(): AppLanguage? = manager.applicationLocales.let {
                    if (it.isEmpty) null else AppLanguage.fromSystem(it[0])
                }
                override fun write(language: AppLanguage) {
                    if (read() != language) {
                        manager.applicationLocales = LocaleList.forLanguageTags(language.languageTag)
                    }
                }
            }
        }
    }
}
