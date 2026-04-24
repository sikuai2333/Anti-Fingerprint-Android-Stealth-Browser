package com.phantommax.app

import android.net.Uri
import java.util.Locale

data class SpoofProfile(
    val name: String,
    val injectSpoof: Boolean,
    val blockTrackers: Boolean,
    val forceDesktopMode: Boolean
)

object SpoofProfileManager {
    private val strictDomains = setOf("web.max.ru", "max.ru")
    private val sensitiveKeywords = listOf("bank", "pay", "wallet", "finance")

    private val strictProfile = SpoofProfile(
        name = "STRICT_MAX",
        injectSpoof = true,
        blockTrackers = true,
        forceDesktopMode = true
    )

    private val balancedProfile = SpoofProfile(
        name = "BALANCED",
        injectSpoof = true,
        blockTrackers = true,
        forceDesktopMode = false
    )

    private val safeProfile = SpoofProfile(
        name = "SAFE_NO_SPOOF",
        injectSpoof = false,
        blockTrackers = true,
        forceDesktopMode = false
    )

    fun resolve(url: String?): SpoofProfile {
        val host = runCatching { Uri.parse(url ?: "").host.orEmpty().lowercase(Locale.US) }
            .getOrDefault("")
        if (host.isBlank()) return balancedProfile
        if (strictDomains.any { host == it || host.endsWith(".$it") }) return strictProfile
        if (sensitiveKeywords.any { host.contains(it) }) return safeProfile
        return balancedProfile
    }
}
