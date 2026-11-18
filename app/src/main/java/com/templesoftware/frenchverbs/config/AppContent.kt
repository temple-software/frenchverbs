package com.templesoftware.frenchverbs.config

import androidx.annotation.StringRes
import com.templesoftware.frenchverbs.R
import java.net.URI

/** Product configuration. Null links display a localized Pending dialog, never a fake policy. */
data class AppContent(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    val guideUrl: String?,
    val privacyUrl: String?,
) {
    init {
        listOfNotNull(guideUrl, privacyUrl).forEach {
            val uri = URI(it)
            require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null)
        }
    }
    companion object {
        const val MORE_APPS_URL = "https://www.nice-education.com"
        val Default = AppContent(R.string.app_name, R.string.about_intro,
            "https://www.nice-education.com/french-verbs/",
            "https://www.nice-education.com/french-verbs/privacy/")
    }
}
