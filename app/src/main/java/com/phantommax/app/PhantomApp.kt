package com.phantommax.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class PhantomApp : Application() {

    companion object {
        @Volatile
        var sessionSeed: Long = 0L
            private set

        var incognitoMode: Boolean = false
        var isDesktopMode: Boolean = false
        var keepSession: Boolean = true
        var killOnExit: Boolean = false
        var destroyOnMinimize: Boolean = false
        var blockCamera: Boolean = true
        var blockMicrophone: Boolean = true
        var blockLocation: Boolean = true
        var savedProxyString: String = ""
        var bypassDomainsCsv: String = "localhost,127.0.0.1,.local"
        
        var blockNetInBackground: Boolean = true
        var showSecurityAlert: Boolean = true
        
        const val DEVELOPER_TGK = "https://t.me/TgUnlock2026"
        const val DEVELOPER_GITHUB = "https://github.com/Genuys"

        fun regenerateSeed() {
            sessionSeed = System.nanoTime() xor (Math.random() * Long.MAX_VALUE).toLong()
        }

        fun loadSettings(context: Context) {
            val prefs = context.getSharedPreferences("PhantomMaxSettings", Context.MODE_PRIVATE)
            incognitoMode = prefs.getBoolean("incognitoMode", false)
            isDesktopMode = prefs.getBoolean("isDesktopMode", false)
            keepSession = prefs.getBoolean("keepSession", true)
            killOnExit = prefs.getBoolean("killOnExit", false)
            destroyOnMinimize = prefs.getBoolean("destroyOnMinimize", false)
            blockCamera = prefs.getBoolean("blockCamera", true)
            blockMicrophone = prefs.getBoolean("blockMicrophone", true)
            blockLocation = prefs.getBoolean("blockLocation", true)
            savedProxyString = prefs.getString("savedProxyString", "") ?: ""
            bypassDomainsCsv = prefs.getString("bypassDomainsCsv", "localhost,127.0.0.1,.local") ?: "localhost,127.0.0.1,.local"
            blockNetInBackground = prefs.getBoolean("blockNetInBackground", true)
            showSecurityAlert = prefs.getBoolean("showSecurityAlert", true)
        }

        fun saveSettings(context: Context) {
            val prefs = context.getSharedPreferences("PhantomMaxSettings", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("incognitoMode", incognitoMode)
                putBoolean("isDesktopMode", isDesktopMode)
                putBoolean("keepSession", keepSession)
                putBoolean("killOnExit", killOnExit)
                putBoolean("destroyOnMinimize", destroyOnMinimize)
                putBoolean("blockCamera", blockCamera)
                putBoolean("blockMicrophone", blockMicrophone)
                putBoolean("blockLocation", blockLocation)
                putString("savedProxyString", savedProxyString)
                putString("bypassDomainsCsv", bypassDomainsCsv)
                putBoolean("blockNetInBackground", blockNetInBackground)
                putBoolean("showSecurityAlert", showSecurityAlert)
                apply()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (sessionSeed == 0L) regenerateSeed()
        loadSettings(this)
        ProxyManager.setBypassRules(bypassDomainsCsv)
        
        if (savedProxyString.isNotEmpty()) {
            val config = ProxyConfig.parse(savedProxyString)
            if (config != null) {
                Thread {
                    ProxyManager.connect(config) { _, _ -> }
                }.start()
            }
        }
    }
}
