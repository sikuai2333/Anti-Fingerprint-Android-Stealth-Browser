package com.phantommax.app

import android.webkit.WebView

class WebViewLifecycleController(
    private val webViewProvider: () -> WebView?,
    private val destroyWebView: () -> Unit,
    private val recreateWebView: () -> Unit,
    private val clearAllData: () -> Unit,
    private val hideSystemUi: () -> Unit
) {
    fun onPause() {
        val webView = webViewProvider() ?: return
        if (PhantomApp.blockNetInBackground) {
            webView.settings.blockNetworkLoads = true
            webView.pauseTimers()
        }
        if (PhantomApp.killOnExit) {
            clearAllData()
        }
        if (PhantomApp.destroyOnMinimize) {
            destroyWebView()
        }
    }

    fun onResume() {
        hideSystemUi()
        val webView = webViewProvider()
        if (PhantomApp.blockNetInBackground) {
            webView?.settings?.blockNetworkLoads = false
            webView?.resumeTimers()
        }
        if (PhantomApp.destroyOnMinimize && webView == null) {
            recreateWebView()
        }
    }
}
