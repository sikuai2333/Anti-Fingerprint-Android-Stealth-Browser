package com.phantommax.app

import androidx.lifecycle.ViewModel

data class SettingsStatusUiState(
    val fingerprintText: String,
    val proxyStatusText: String,
    val proxyStatusColor: Int,
    val ipText: String,
    val pingText: String,
    val countryText: String,
    val proxyTypeText: String
)

class SettingsStateViewModel : ViewModel() {
    fun buildStatusState(
        proxy: ProxyConfig?,
        ua: String,
        isDesktopMode: Boolean,
        seed: Long,
        currentUrl: String?,
        detectedIp: String,
        lastPingMs: Long,
        detectedCountry: String
    ): SettingsStatusUiState {
        val chromeVer = Regex("Chrome/(\\d+)").find(ua)?.groupValues?.get(1) ?: "?"
        val mode = if (isDesktopMode) "Desktop" else "Mobile"
        val profile = SpoofProfileManager.resolve(currentUrl)
        val route = ProxyManager.routeForUrl(currentUrl)
        val fingerprint = "Chrome $chromeVer | $mode | Profile:${profile.name} | Seed: ${seed and 0xFFFF}\n${ua.take(80)}..."

        return if (proxy != null) {
            SettingsStatusUiState(
                fingerprintText = fingerprint,
                proxyStatusText = "✅ Подключено",
                proxyStatusColor = 0xFF00E676.toInt(),
                ipText = detectedIp.ifEmpty { "⏳ определяется..." },
                pingText = if (lastPingMs >= 0) "$lastPingMs мс" else "—",
                countryText = detectedCountry.ifEmpty { "⏳ определяется..." },
                proxyTypeText = "${proxy.type.name} | Route:$route"
            )
        } else {
            SettingsStatusUiState(
                fingerprintText = fingerprint,
                proxyStatusText = "⛔ Отключено",
                proxyStatusColor = 0xFFFF5252.toInt(),
                ipText = "—",
                pingText = "—",
                countryText = "—",
                proxyTypeText = "Route:$route"
            )
        }
    }
}
