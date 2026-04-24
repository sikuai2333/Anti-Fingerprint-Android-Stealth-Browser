package com.phantommax.app

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout

import androidx.webkit.ProxyConfig as WebkitProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

class MainActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private lateinit var webViewContainer: FrameLayout
    private lateinit var fabSettings: FloatingActionButton
    private lateinit var fabHelp: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingLayout: LinearLayout
    private lateinit var qrCardLayout: LinearLayout
    private lateinit var btnSaveQr: Button
    private lateinit var btnHideQr: ImageButton
    private var qrDismissed = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupViews()
        setupBackPressed()

        if (PhantomApp.showSecurityAlert && ProxyManager.currentProxy == null) {
            showSecurityAlertBottomSheet()
        } else {
            if (webView == null) {
                createWebView()
            }
        }

        animateEntrance()
    }

    private fun showSecurityAlertBottomSheet(customMessage: String? = null) {
        val dialog = BottomSheetDialog(this, R.style.Theme_PhantomMAX_BottomSheet)
        val view = layoutInflater.inflate(R.layout.dialog_security_alert, null)
        dialog.setContentView(view)
        dialog.setCancelable(false)

        val tvDialogMessage = view.findViewById<TextView>(R.id.tvDialogMessage)
        if (customMessage != null) {
            tvDialogMessage.text = customMessage
        }

        var goingToSettings = false
        view.findViewById<Button>(R.id.btnDialogConnect).setOnClickListener {
            goingToSettings = true
            dialog.dismiss()
            showSettingsSheet()
        }
        view.findViewById<Button>(R.id.btnDialogContinue).setOnClickListener {
            dialog.dismiss()
            createWebView()
        }
        dialog.setOnDismissListener {
            if (!goingToSettings && webView == null) {
                createWebView()
            }
        }
        dialog.show()
    }

    private fun applyNativeProxy(onDone: (() -> Unit)? = null) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            onDone?.invoke()
            return
        }

        val executor = java.util.concurrent.Executor { task -> runOnUiThread { task.run() } }
        val config = ProxyManager.currentProxy

        if (config != null) {
            val url = when {
                ProxyManager.isVlessActive() -> "socks5://127.0.0.1:10808"
                config.type == ProxyConfig.Type.SOCKS5 -> "socks5://${config.host}:${config.port}"
                config.type == ProxyConfig.Type.HTTP -> "http://${config.host}:${config.port}"
                else -> ""
            }
            if (url.isNotEmpty()) {
                try {
                    val proxyConfig = WebkitProxyConfig.Builder()
                        .addProxyRule(url)
                        .build()
                    ProxyController.getInstance().setProxyOverride(proxyConfig, executor) {
                        onDone?.invoke()
                    }
                } catch (e: Exception) {
                    onDone?.invoke()
                }
            } else {
                try {
                    ProxyController.getInstance().clearProxyOverride(executor) { onDone?.invoke() }
                } catch (e: Exception) {
                    onDone?.invoke()
                }
            }
        } else {
            try {
                ProxyController.getInstance().clearProxyOverride(executor) { onDone?.invoke() }
            } catch (e: Exception) {
                onDone?.invoke()
            }
        }
    }

    private fun hideSystemUI() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }

    private fun setupViews() {
        webViewContainer = findViewById(R.id.webViewContainer)
        fabSettings = findViewById(R.id.fabSettings)
        fabHelp = findViewById(R.id.fabHelp)
        progressBar = findViewById(R.id.progressBar)
        loadingLayout = findViewById(R.id.loadingLayout)
        qrCardLayout = findViewById(R.id.qrCardLayout)
        btnSaveQr = findViewById(R.id.btnSaveQr)
        btnHideQr = findViewById(R.id.btnHideQr)

        fabSettings.setOnClickListener { showSettingsSheet() }
        fabHelp.setOnClickListener {
            startActivity(Intent(this, HowToActivity::class.java))
        }

        btnHideQr.setOnClickListener {
            qrDismissed = true
            qrCardLayout.animate().translationY(qrCardLayout.height.toFloat()).setDuration(300).withEndAction {
                qrCardLayout.visibility = View.GONE
            }.start()
        }

        btnSaveQr.setOnClickListener { saveQrToGallery() }
    }

    private fun animateEntrance() {
        fabSettings.alpha = 0f
        fabSettings.translationY = 50f
        fabSettings.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(200).start()

        fabHelp.alpha = 0f
        fabHelp.translationY = 50f
        fabHelp.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(300).start()
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView?.canGoBack() == true) {
                    webView?.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    @SuppressLint("SetJavaScriptEnabled", "RequiresFeature")
    private fun createWebView() {
        mainHandler.removeCallbacksAndMessages(null)
        webView?.destroy()
        webViewContainer.removeAllViews()
        qrDismissed = false

        PhantomApp.regenerateSeed()

        loadingLayout.visibility = View.VISIBLE
        loadingLayout.alpha = 1f
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 5

        val wv = WebView(this)

        val settings = wv.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.userAgentString = HeaderManager.getUA(PhantomApp.isDesktopMode)
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mediaPlaybackRequiresUserGesture = false
        settings.blockNetworkImage = false
        settings.loadsImagesAutomatically = true
        settings.setGeolocationEnabled(false)
        settings.javaScriptCanOpenWindowsAutomatically = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = false
        }

        wv.setInitialScale(100)
        wv.setBackgroundColor(0xFF0A0A14.toInt())

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)

        wv.webViewClient = PhantomWebViewClient(
            onPageLoadStarted = {
                runOnUiThread {
                    loadingLayout.visibility = View.VISIBLE
                    loadingLayout.alpha = 1f
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = 30
                }
            },
            onPageLoadFinished = {
                runOnUiThread {
                    progressBar.progress = 100
                    loadingLayout.animate().alpha(0f).setDuration(400).withEndAction {
                        loadingLayout.visibility = View.GONE
                        progressBar.visibility = View.GONE
                    }.start()
                    startQrPolling()
                }
            }
        )
        wv.webChromeClient = PhantomChromeClient()

        webViewContainer.addView(wv, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        webView = wv

        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            try {
                // (Feature disabled to prevent SPA boot race conditions)
            } catch (_: Exception) {}
        }

        if (PhantomApp.incognitoMode) {
            clearAllData()
        }

        var urlLoaded = false
        fun doLoad() {
            if (!urlLoaded && webView === wv) {
                urlLoaded = true
                val entryUrl = "https://web.max.ru"
                val profile = SpoofProfileManager.resolve(entryUrl)
                wv.loadUrl(entryUrl, HeaderManager.getHeaders(profile.forceDesktopMode || PhantomApp.isDesktopMode))
            }
        }

        val loadRunnable = Runnable { doLoad() }
        mainHandler.postDelayed(loadRunnable, 700)

        applyNativeProxy {
            mainHandler.removeCallbacks(loadRunnable)
            doLoad()
        }
    }

    private var qrPollRunnable: Runnable? = null

    private fun startQrPolling() {
        stopQrPolling()
        qrPollRunnable = object : Runnable {
            override fun run() {
                detectQrOnPage()
                webView?.postDelayed(this, 1000)
            }
        }
        webView?.postDelayed(qrPollRunnable, 500)
    }

    private fun stopQrPolling() {
        qrPollRunnable?.let { webView?.removeCallbacks(it) }
        qrPollRunnable = null
    }

    private fun detectQrOnPage() {
        webView?.evaluateJavascript("""
            (function(){
                var url = window.location.href || '';
                var path = window.location.pathname || '';
                var isLoggedIn = false;
                try {
                    if(document.cookie.indexOf('session') !== -1 || document.cookie.indexOf('auth') !== -1) { isLoggedIn = true; }
                    var chatEls = document.querySelectorAll('[class*="chat"],[class*="message"],[class*="dialog"],[class*="inbox"],[class*="nav"]');
                    if(chatEls.length > 5 && path !== '/' && path.indexOf('login') === -1 && path.indexOf('auth') === -1) { isLoggedIn = true; }
                    var title = document.title.toLowerCase();
                    if(url.indexOf('web.max.ru/') >= 0 && title.indexOf('вход') === -1 && title.indexOf('login') === -1 && chatEls.length > 0) { isLoggedIn = true; }
                } catch(e){}
                if(isLoggedIn) return 'logged_in';
                var canvases = document.querySelectorAll('canvas');
                for(var i=0; i<canvases.length; i++){
                    var c = canvases[i]; var rect = c.getBoundingClientRect();
                    if(rect.width >= 100 && rect.width <= 600 && rect.height >= 100 && rect.height <= 600) {
                        if(rect.width / rect.height > 0.8 && rect.width / rect.height < 1.2) return 'found';
                    }
                }
                var svgs = document.querySelectorAll('svg');
                for(var j=0; j<svgs.length; j++){
                    var s = svgs[j]; var sr = s.getBoundingClientRect();
                    if(sr.width >= 50 && sr.width <= 600 && sr.height >= 50 && sr.height <= 600) {
                        if(sr.width / sr.height > 0.7 && sr.width / sr.height < 1.3) {
                            if(s.querySelectorAll('rect,path').length > 20) return 'found';
                        }
                    }
                }
                var imgs = document.querySelectorAll('img');
                for(var k=0; k<imgs.length; k++){
                    var im = imgs[k]; var src = (im.src || '').toLowerCase(); var alt = (im.alt || '').toLowerCase();
                    if(src.indexOf('qr') >= 0 || alt.indexOf('qr') >= 0 || src.indexOf('data:image') === 0 || im.className.toLowerCase().indexOf('qr') >= 0) {
                        var ir = im.getBoundingClientRect();
                        if(ir.width >= 50 && ir.width <= 600 && ir.height >= 50 && ir.height <= 600) return 'found';
                    }
                }
                return 'none';
            })();
        """) { result ->
            val r = result?.replace("\"", "") ?: ""
            runOnUiThread {
                when (r) {
                    "logged_in" -> {
                        stopQrPolling()
                        if (fabHelp.isOrWillBeShown) fabHelp.hide()
                        if (qrCardLayout.visibility == View.VISIBLE) {
                            qrCardLayout.animate().translationY(qrCardLayout.height.toFloat()).setDuration(300).withEndAction {
                                qrCardLayout.visibility = View.GONE
                            }.start()
                        }
                    }
                    "found" -> {
                        if (fabHelp.isOrWillBeHidden) fabHelp.show()
                        if (!qrDismissed && qrCardLayout.visibility != View.VISIBLE) {
                            qrCardLayout.visibility = View.VISIBLE
                            qrCardLayout.translationY = qrCardLayout.height.toFloat()
                            qrCardLayout.animate().translationY(0f).setDuration(400).start()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun saveQrToGallery() {
        val wv = webView ?: return
        val bitmap = Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        wv.draw(canvas)

        val filename = "PhantomMAX_QR_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhantomMAX")
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            Toast.makeText(this, "QR + Экран сохранён в галерею", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Не удалось сохранить (проверьте разрешения)", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("InflateParams")
    private fun showSettingsSheet() {
        val dialog = BottomSheetDialog(this, R.style.Theme_PhantomMAX_BottomSheet)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_settings, null)
        dialog.setContentView(view)

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val proxyPage = view.findViewById<LinearLayout>(R.id.proxyPage)
        val sessionPage = view.findViewById<LinearLayout>(R.id.sessionPage)
        val statusPage = view.findViewById<LinearLayout>(R.id.statusPage)
        val logPage = view.findViewById<LinearLayout>(R.id.logPage)

        val editProxy = view.findViewById<EditText>(R.id.editProxy)
        val btnConnect = view.findViewById<Button>(R.id.btnConnect)
        val btnDisconnect = view.findViewById<Button>(R.id.btnDisconnect)
        val tvProxyStatus = view.findViewById<TextView>(R.id.tvProxyStatus)
        val tvSpoofLog = view.findViewById<TextView>(R.id.tvSpoofLog)

        val switchKeep = view.findViewById<SwitchCompat>(R.id.switchKeepSession)
        val switchKill = view.findViewById<SwitchCompat>(R.id.switchKillOnExit)
        val switchIncognito = view.findViewById<SwitchCompat>(R.id.switchIncognito)
        val switchDesktopMode = view.findViewById<SwitchCompat>(R.id.switchDesktopMode)
        val switchBlockCamera = view.findViewById<SwitchCompat>(R.id.switchBlockCamera)
        val switchBlockMicrophone = view.findViewById<SwitchCompat>(R.id.switchBlockMicrophone)
        val switchBlockLocation = view.findViewById<SwitchCompat>(R.id.switchBlockLocation)

        val tvIp = view.findViewById<TextView>(R.id.tvIp)
        val tvPing = view.findViewById<TextView>(R.id.tvPing)
        val tvCountry = view.findViewById<TextView>(R.id.tvCountry)
        val tvProxyType = view.findViewById<TextView>(R.id.tvProxyType)
        val tvSpoofFingerprint = view.findViewById<TextView>(R.id.tvSpoofFingerprint)
        val btnRefreshIp = view.findViewById<Button>(R.id.btnRefreshIp)

        val advancedPage = view.findViewById<LinearLayout>(R.id.advancedPage)
        val developerPage = view.findViewById<LinearLayout>(R.id.developerPage)
        val containerSuggestions = view.findViewById<LinearLayout>(R.id.containerSuggestions)
        val switchBlockNetBack = view.findViewById<SwitchCompat>(R.id.switchBlockNetBack)
        val switchShowAlert = view.findViewById<SwitchCompat>(R.id.switchShowAlert)
        val btnTgDev = view.findViewById<Button>(R.id.btnTgDev)
        val btnGitDev = view.findViewById<Button>(R.id.btnGitDev)

        fun refreshSuggestions() {
            containerSuggestions.removeAllViews()
            Thread {
                val rawList = listOf(
                    "tg://proxy?server=85.120.81.163&port=443&secret=e79ddd99773fdcac6ea69729d0f9d0f1",
                    "tg://proxy?server=slark.shukafish.ru&port=443&secret=f5199e949c0f23bc887581218ad8c1e6",
                    "tg://proxy?server=104.248.26.196&port=443&secret=eec80ff604fa45408f1d152624d3bffcf276616e2e6e616a76612e636f6d",
                    "tg://proxy?server=216.230.234.3&port=8443&secret=cd25966d58b3a452ea24ed9ca78a2799",
                    "tg://proxy?server=130.0.239.75&port=443&secret=eef9f48d41044f0225c1af64490ce88cda7777772e7a6f6f6069742e6972",
                    "tg://proxy?server=116.202.2.11&port=443&secret=dd79e344818749bd7ac519130220c25d09",
                    "tg://proxy?server=43.250.53.22&port=443&secret=4fc30d1222972a04067e805a5134cb57",
                    "tg://proxy?server=142.54.189.106&port=443&secret=ee09db815a6d82a31fda76f872230c69d7706b676275696c642e6f7267"
                )
                rawList.forEach { raw ->
                    val config = ProxyConfig.parse(raw) ?: return@forEach
                    runOnUiThread {
                        val item = layoutInflater.inflate(R.layout.item_proxy_suggestion, containerSuggestions, false)
                        item.alpha = 0f; item.translationX = 100f
                        item.findViewById<TextView>(R.id.tvProxyName).text = "Node ${config.host.take(12)}..."
                        item.findViewById<TextView>(R.id.tvProxyHost).text = "${config.host}:${config.port}"
                        val tvPingVal = item.findViewById<TextView>(R.id.tvProxyPing)
                        tvPingVal.text = "---"
                        item.setOnClickListener { editProxy.setText(raw); btnConnect.performClick() }
                        containerSuggestions.addView(item)
                        item.animate().alpha(1f).translationX(0f).setDuration(400)
                            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                        ProxyManager.ping(config) { p ->
                            runOnUiThread {
                                if (p >= 0) {
                                    tvPingVal.text = "$p мс"
                                    tvPingVal.setTextColor(if (p < 300) 0xFF00E676.toInt() else 0xFFFFD740.toInt())
                                } else {
                                    containerSuggestions.removeView(item)
                                }
                            }
                        }
                    }
                }
            }.start()
        }

        refreshSuggestions()

        editProxy.maxLines = 5
        editProxy.isSingleLine = false
        editProxy.setHorizontallyScrolling(false)
        editProxy.setText(PhantomApp.savedProxyString)

        switchKeep.isChecked = PhantomApp.keepSession
        switchKill.isChecked = PhantomApp.killOnExit
        switchIncognito.isChecked = PhantomApp.incognitoMode
        switchDesktopMode.isChecked = PhantomApp.isDesktopMode
        switchBlockCamera.isChecked = PhantomApp.blockCamera
        switchBlockMicrophone.isChecked = PhantomApp.blockMicrophone
        switchBlockLocation.isChecked = PhantomApp.blockLocation
        switchBlockNetBack.isChecked = PhantomApp.blockNetInBackground
        switchShowAlert.isChecked = PhantomApp.showSecurityAlert

        btnTgDev.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(PhantomApp.DEVELOPER_TGK))) }
        btnGitDev.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(PhantomApp.DEVELOPER_GITHUB))) }

        fun saveAppPrefs() { PhantomApp.saveSettings(this) }

        switchKeep.setOnCheckedChangeListener { _, c -> PhantomApp.keepSession = c; saveAppPrefs() }
        switchKill.setOnCheckedChangeListener { _, c -> PhantomApp.killOnExit = c; saveAppPrefs() }
        switchIncognito.setOnCheckedChangeListener { _, c -> PhantomApp.incognitoMode = c; saveAppPrefs() }
        switchDesktopMode.setOnCheckedChangeListener { _, c -> PhantomApp.isDesktopMode = c; saveAppPrefs(); createWebView() }
        switchBlockCamera.setOnCheckedChangeListener { _, c -> PhantomApp.blockCamera = c; saveAppPrefs() }
        switchBlockMicrophone.setOnCheckedChangeListener { _, c -> PhantomApp.blockMicrophone = c; saveAppPrefs() }
        switchBlockLocation.setOnCheckedChangeListener { _, c -> PhantomApp.blockLocation = c; saveAppPrefs() }
        switchBlockNetBack.setOnCheckedChangeListener { _, c -> PhantomApp.blockNetInBackground = c; saveAppPrefs() }
        switchShowAlert.setOnCheckedChangeListener { _, c -> PhantomApp.showSecurityAlert = c; saveAppPrefs() }
        ProxyManager.setBypassRules(PhantomApp.bypassDomainsCsv)

        fun updateLogs() {
            val log = SpoofingEngine.getSpoofLog(PhantomApp.sessionSeed, PhantomApp.isDesktopMode)
            val metrics = SpoofMetrics.snapshotText()
            runOnUiThread { tvSpoofLog.text = (log.ifBlank { "Пусто" } + "\n\n" + metrics).trim() }
        }

        fun showPage(index: Int) {
            proxyPage.visibility = if (index == 0) View.VISIBLE else View.GONE
            sessionPage.visibility = if (index == 1) View.VISIBLE else View.GONE
            statusPage.visibility = if (index == 2) View.VISIBLE else View.GONE
            logPage.visibility = if (index == 3) View.VISIBLE else View.GONE
            advancedPage.visibility = if (index == 4) View.VISIBLE else View.GONE
            developerPage.visibility = if (index == 5) View.VISIBLE else View.GONE
            if (index == 3) updateLogs()
        }

        fun updateStatus() {
            val proxy = ProxyManager.currentProxy
            val ua = HeaderManager.getUA(PhantomApp.isDesktopMode)
            val chromeVer = Regex("Chrome/(\\d+)").find(ua)?.groupValues?.get(1) ?: "?"
            val mode = if (PhantomApp.isDesktopMode) "Desktop" else "Mobile"
            val profile = SpoofProfileManager.resolve(webView?.url)
            tvSpoofFingerprint.text = "Chrome $chromeVer | $mode | Profile:${profile.name} | Seed: ${PhantomApp.sessionSeed and 0xFFFF}\n${ua.take(80)}..."
            val route = ProxyManager.routeForUrl(webView?.url)

            if (proxy != null) {
                tvProxyStatus.text = "✅ Подключено"
                tvProxyStatus.setTextColor(0xFF00E676.toInt())
                tvIp.text = ProxyManager.detectedIp.ifEmpty { "⏳ определяется..." }
                tvPing.text = if (ProxyManager.lastPingMs >= 0) "${ProxyManager.lastPingMs} мс" else "—"
                tvCountry.text = ProxyManager.detectedCountry.ifEmpty { "⏳ определяется..." }
                tvProxyType.text = "${proxy.type.name} | Route:$route"
            } else {
                tvProxyStatus.text = "⛔ Отключено"
                tvProxyStatus.setTextColor(0xFFFF5252.toInt())
                tvIp.text = "—"; tvPing.text = "—"; tvCountry.text = "—"; tvProxyType.text = "Route:$route"
            }
        }

        btnRefreshIp.setOnClickListener {
            btnRefreshIp.isEnabled = false
            tvIp.text = "⏳ запрос..."
            tvCountry.text = "⏳ ..."
            ProxyManager.refreshIpInfo()
            view.postDelayed({ updateStatus(); btnRefreshIp.isEnabled = true }, 4000)
        }

        updateStatus()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val i = tab?.position ?: 0
                showPage(i)
                if (i == 0) refreshSuggestions()
                if (i == 2) updateStatus()
                if (i == 3) updateLogs()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                val i = tab?.position ?: 0
                if (i == 3) updateLogs()
                if (i == 0) refreshSuggestions()
            }
        })

        tabLayout.getTabAt(0)?.select()
        showPage(0)

        btnConnect.setOnClickListener {
            val input = editProxy.text.toString()
            if (input.isBlank()) { Toast.makeText(this, "Введите прокси", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val config = ProxyConfig.parse(input)
            if (config == null) { Toast.makeText(this, "Неверный формат", Toast.LENGTH_LONG).show(); return@setOnClickListener }

            PhantomApp.savedProxyString = input
            saveAppPrefs()
            tvProxyStatus.text = "ОЖИДАЙТЕ ЗАГРУЗКИ...\nСреднее время: ~3.5 сек"
            tvProxyStatus.setTextColor(0xFFFFD740.toInt())
            btnConnect.isEnabled = false
            loadingLayout.visibility = View.VISIBLE
            loadingLayout.alpha = 1f

            ProxyManager.connect(config) { success, error ->
                runOnUiThread {
                    btnConnect.isEnabled = true
                    if (success) {
                        tvProxyStatus.text = "✅ Подключено"
                        tvProxyStatus.setTextColor(0xFF00E676.toInt())
                        updateStatus()
                        createWebView()
                        dialog.dismiss()
                    } else {
                        loadingLayout.visibility = View.GONE
                        tvProxyStatus.text = "❌ Ошибка: $error"
                        tvProxyStatus.setTextColor(0xFFFF5252.toInt())
                        showSecurityAlertBottomSheet("Не удалось подключить прокси. Ваш реальный IP может быть виден.")
                    }
                }
            }
        }

        btnDisconnect.setOnClickListener {
            ProxyManager.disconnect()
            PhantomApp.savedProxyString = ""
            saveAppPrefs()
            editProxy.setText("")
            tvProxyStatus.text = "⛔ Отключено"
            tvProxyStatus.setTextColor(0xFFFF5252.toInt())
            updateStatus()
            Toast.makeText(this, "Прокси отключён", Toast.LENGTH_SHORT).show()
            applyNativeProxy {
                dialog.dismiss()
                showSecurityAlertBottomSheet("Вы отключили прокси. Ваш реальный IP виден. Продолжить без защиты?")
            }
        }

        dialog.show()
    }

    private fun clearAllData() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        webView?.clearCache(true)
        webView?.clearHistory()
        webView?.clearFormData()
    }

    private var isAppInBackground = false

    override fun onPause() {
        super.onPause()
        isAppInBackground = true
        if (PhantomApp.blockNetInBackground) {
            webView?.settings?.blockNetworkLoads = true
            webView?.pauseTimers()
        }
        if (PhantomApp.killOnExit) clearAllData()
        if (PhantomApp.destroyOnMinimize) {
            webView?.destroy()
            webView = null
        }
    }

    override fun onResume() {
        super.onResume()
        isAppInBackground = false
        hideSystemUI()
        if (PhantomApp.blockNetInBackground) {
            webView?.settings?.blockNetworkLoads = false
            webView?.resumeTimers()
        }
        if (PhantomApp.destroyOnMinimize && webView == null) {
            createWebView()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        stopQrPolling()
        webView?.destroy()
        webView = null
        ProxyManager.disconnect()
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
