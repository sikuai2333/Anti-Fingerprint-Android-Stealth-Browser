package com.phantommax.app

import android.graphics.Bitmap
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewFeature
import java.io.ByteArrayInputStream

class PhantomWebViewClient(
    private val onPageLoadStarted: (() -> Unit)? = null,
    private val onPageLoadFinished: (() -> Unit)? = null
) : WebViewClient() {

    private val geoBlockScript = """(function(){
if(window.__geoBlocked)return;window.__geoBlocked=true;
try{Object.defineProperty(navigator,'geolocation',{get:function(){return{
getCurrentPosition:function(s,e,o){if(e)e({code:1,message:'User denied Geolocation'});},
watchPosition:function(s,e,o){if(e)e({code:1,message:'User denied Geolocation'});return 0;},
clearWatch:function(){}
};},configurable:true});}catch(ex){}
try{if(navigator.permissions){var _oq=navigator.permissions.query.bind(navigator.permissions);navigator.permissions.query=function(d){if(d&&(d.name==='geolocation'||d.name==='camera'||d.name==='microphone'||d.name==='notifications'))return Promise.resolve({state:'denied',onchange:null,addEventListener:function(){},removeEventListener:function(){}});return _oq(d);};}}catch(ex){}
})();"""

    private fun getSpoofScript(forceDesktop: Boolean) = SpoofingEngine.generateScript(
        PhantomApp.sessionSeed, forceDesktop || PhantomApp.isDesktopMode
    )

    private fun injectScript(view: WebView?, stage: String, script: String) {
        if (view == null) return
        SpoofMetrics.recordAttempt(stage)
        try {
            view.evaluateJavascript(script) {
                SpoofMetrics.recordSuccess()
            }
        } catch (e: Exception) {
            SpoofMetrics.recordFailure(e.message ?: "unknown script error")
        }
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString().orEmpty()
        val profile = SpoofProfileManager.resolve(url)
        if (profile.blockTrackers && url.isNotEmpty() && TrackerBlocker.shouldBlock(url)) {
            return emptyResponse(url)
        }
        return null
    }

    private fun emptyResponse(url: String): WebResourceResponse {
        val ext = MimeTypeMap.getFileExtensionFromUrl(url).lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "text/plain"
        return WebResourceResponse(mime, "UTF-8", ByteArrayInputStream(ByteArray(0)))
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        if (url.startsWith("tg://") || url.startsWith("intent://")) return true
        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageLoadStarted?.invoke()
        val profile = SpoofProfileManager.resolve(url)
        if (profile.injectSpoof) {
            injectScript(view, "onPageStarted:spoof:${profile.name}", getSpoofScript(profile.forceDesktopMode))
        }
        if (PhantomApp.blockLocation && profile.injectSpoof) {
            injectScript(view, "onPageStarted:geo", geoBlockScript)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageLoadFinished?.invoke()
        val profile = SpoofProfileManager.resolve(url)
        if (PhantomApp.blockLocation && profile.injectSpoof) {
            injectScript(view, "onPageFinished:geo", geoBlockScript)
        }
        if (profile.injectSpoof) {
            injectScript(view, "onPageFinished:spoof:${profile.name}", getSpoofScript(profile.forceDesktopMode))
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: android.webkit.WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val errCode = error?.errorCode ?: -1
            val errDesc = error?.description?.toString() ?: "Unknown"
            android.widget.Toast.makeText(view?.context, "Ошибка сети: $errCode $errDesc", android.widget.Toast.LENGTH_LONG).show()
            val html = """<!DOCTYPE html>
<html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<style>*{margin:0;padding:0;box-sizing:border-box}body{background:#0A0A14;color:#fff;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;font-family:system-ui,sans-serif;padding:24px;text-align:center}h2{color:#FF5252;margin-bottom:16px;font-size:20px}p{color:#aaa;margin:8px 0;font-size:14px}b{color:#7C4DFF}code{background:#1a1a2e;padding:8px 16px;border-radius:8px;color:#FF5252;font-size:12px;display:block;margin-top:16px}</style>
</head><body>
<h2>⚠️ Ошибка соединения</h2>
<p>Не удалось загрузить:</p>
<p><b>${request.url}</b></p>
<p>Проверьте прокси или интернет-соединение.</p>
<code>Код: $errCode | $errDesc</code>
</body></html>"""
            view?.loadDataWithBaseURL(request.url.toString(), html, "text/html", "UTF-8", null)
            onPageLoadFinished?.invoke()
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: android.webkit.WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request?.isForMainFrame == true) {
            val code = errorResponse?.statusCode ?: -1
            val phrase = errorResponse?.reasonPhrase ?: "HTTP Error"
            android.widget.Toast.makeText(view?.context, "Ошибка HTTP: $code $phrase", android.widget.Toast.LENGTH_LONG).show()
            val html = """<!DOCTYPE html>
<html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<style>*{margin:0;padding:0;box-sizing:border-box}body{background:#0A0A14;color:#fff;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;font-family:system-ui,sans-serif;padding:24px;text-align:center}h2{color:#FF5252;margin-bottom:16px;font-size:20px}p{color:#aaa;margin:8px 0;font-size:14px}b{color:#7C4DFF}code{background:#1a1a2e;padding:8px 16px;border-radius:8px;color:#FF5252;font-size:12px;display:block;margin-top:16px}</style>
</head><body>
<h2>⚠️ Ошибка сервера</h2>
<p>Сервер ${request.url.host} отклонил запрос:</p>
<p><b>${request.url}</b></p>
<p>Проверьте доступность сайта (возможно, он заблокирован).</p>
<code>HTTP $code | $phrase</code>
</body></html>"""
            view?.loadDataWithBaseURL(request.url.toString(), html, "text/html", "UTF-8", null)
            onPageLoadFinished?.invoke()
        }
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: android.webkit.SslErrorHandler?,
        error: android.net.http.SslError?
    ) {
        handler?.cancel()
    }
}
