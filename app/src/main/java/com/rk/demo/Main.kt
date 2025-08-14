package com.rk.demo

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.setPadding
import com.rk.extension.Extension
import com.rk.extension.ExtensionAPI
import com.rk.pluginApi.PluginApi
import com.rk.xededitor.BuildConfig
import com.rk.xededitor.MainActivity.MainActivity
import com.rk.xededitor.MainActivity.tabs.core.CoreFragment

// Main-thread helper
private inline fun onUi(crossinline block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) block()
    else Handler(Looper.getMainLooper()).post { block() }
}

class Main : ExtensionAPI() {

    val tabId = "browser"
    val tabName = "Browser"

    override fun onPluginLoaded(extension: Extension) {
        Log.d("Main", "onPluginLoaded")
        PluginApi.registerTab(id = tabId, tabName = tabName) { tabFragment ->
            object : CoreFragment() {
                var webView: WebView? = null

                override fun getView(): View? {
                    val activity = MainActivity.activityRef.get()!!

                    val container = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                    }

                    val topBar = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(8)
                    }

                    val urlInput = EditText(activity).apply {
                        hint = "Enter URL (e.g. example.com or https://example.com)"
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                        isSingleLine = true
                    }

                    val goButton = Button(activity).apply {
                        text = "Enter"
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    topBar.addView(urlInput)
                    topBar.addView(goButton)

                    val browser = WebView(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            0,
                            1f
                        )

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(true)
                        settings.loadsImagesAutomatically = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.userAgentString = settings.userAgentString

                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webChromeClient = WebChromeClient()

                        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                            val request = DownloadManager.Request(Uri.parse(url))
                            request.setMimeType(mimetype)
                            request.addRequestHeader("User-Agent", userAgent)
                            request.setDescription("Downloading file...")
                            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
                            request.allowScanningByMediaScanner()
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            request.setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                URLUtil.guessFileName(url, contentDisposition, mimetype)
                            )
                            val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)
                        }

                        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
                    }

                    // Hardened Eruda injector
                    fun getErudaInjectorJs(): String = """(function () {  try {    if (window.__ERUDA_INJECTED__) return;     window.__ERUDA_INJECTED__ = true;    if (typeof eruda !== 'undefined') {      try { eruda.init({ defaults: { theme: 'AMOLED' } }); } catch (e) {}      return;    }    if (!document.getElementById('__eruda_loader__')) {      var style = document.createElement('style');      style.textContent = `@keyframes eruda-fade-in {0% {opacity: 0; transform: translateY(20px);}100% {opacity: 1; transform: translateY(0);}}@keyframes eruda-spin {0% {transform: rotate(0deg);}100% {transform: rotate(360deg);}}.eruda-loader { position: fixed; bottom: 35px; right: 20px; width: 50px; height: 50px; background: rgba(0,0,0,0.7); color: #00aa00; border-radius: 50%; display: flex; align-items: center; justify-content: center; z-index: 2147483647; animation: eruda-fade-in 0.5s ease-out; box-shadow: 0 4px 12px rgba(0,0,0,0.15);}.eruda-loader::after { content: ""; width: 35px; height: 30px; border: 3px solid rgba(0,170,0,0.3); border-top-color: #00aa00; border-radius: 50%; animation: eruda-spin 0.8s linear infinite; }.eruda-loader-text { position: absolute; bottom: -35px; width: 100%; text-align: center; font-size: 12px; color: #00aa00; }`;      (document.head || document.documentElement).appendChild(style);      var loader = document.createElement('div');      loader.id = '__eruda_loader__';      loader.className = 'eruda-loader';      var loaderText = document.createElement('div');      loaderText.className = 'eruda-loader-text';      loaderText.textContent = 'Loading Debugger...';      loader.appendChild(loaderText);      (document.body || document.documentElement).appendChild(loader);    }    var s = document.createElement('script');    s.src = 'https://cdn.jsdelivr.net/npm/eruda';    s.onload = function () {      try {        if (typeof eruda === 'undefined') throw new Error('eruda missing');        eruda.init({ defaults: { theme: 'AMOLED' } });        var loader = document.getElementById('__eruda_loader__');        if (loader) {          var txt = loader.querySelector('.eruda-loader-text');          if (txt) txt.textContent = 'Debugger Ready!';          setTimeout(function(){            if (loader && loader.style) loader.style.transition = 'opacity .5s';            if (loader) loader.style.opacity = '0';            setTimeout(function(){ if (loader && loader.remove) loader.remove(); }, 500);          }, 1000);        }      } catch (e) {        var loader = document.getElementById('__eruda_loader__');        if (loader) {          var txt = loader.querySelector('.eruda-loader-text');          if (txt) txt.textContent = 'Init error';          setTimeout(function(){ loader.remove && loader.remove(); }, 1500);        }      }    };    s.onerror = function () {      var loader = document.getElementById('__eruda_loader__');      if (loader) {        var txt = loader.querySelector('.eruda-loader-text');        if (txt) txt.textContent = 'Failed to load!';        setTimeout(function(){ loader.remove && loader.remove(); }, 1500);      }    };    (document.head || document.documentElement).appendChild(s);  } catch (e) { try { console.error(e); } catch(_) {} }})();""".trimIndent()

                    fun injectErudaInto(view: WebView?) {
                        if (view == null) return
                        val js = getErudaInjectorJs()
                        view.post {
                            try {
                                view.evaluateJavascript(js, null)
                            } catch (e: Exception) {
                                Log.e("Main", "injectErudaInto error: ${e.message}", e)
                            }
                        }
                    }

                    fun normalizeUrl(raw: String): String {
                        var r = raw.trim()
                        if (r.isEmpty()) return "about:blank"
                        if (!r.startsWith("http://") && !r.startsWith("https://")) r = "https://$r"
                        return r
                    }

                    browser.webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            injectErudaInto(view)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            injectErudaInto(view)
                        }
                    }

                    goButton.setOnClickListener {
                        val finalUrl = normalizeUrl(urlInput.text?.toString() ?: "")
                        browser.post { browser.loadUrl(finalUrl) }
                    }
                    urlInput.setOnEditorActionListener { v, _, _ ->
                        val finalUrl = normalizeUrl(v.text?.toString() ?: "")
                        browser.post { browser.loadUrl(finalUrl) }
                        true
                    }

                    browser.loadUrl("about:blank")

                    container.addView(topBar)
                    container.addView(browser)

                    webView = browser
                    return container
                }

                override fun onDestroy() {
                    webView?.apply {
                        onUi {
                            loadUrl("about:blank")
                            stopLoading()
                            webChromeClient = null
                            webViewClient = WebViewClient()
                            destroy()
                        }
                    }
                    webView = null
                }

                override fun onCreate() {}
                override fun onClosed() { onDestroy() }
            }
        }

        onUi {
            MainActivity.activityRef.get()?.menu?.apply {
                val browserItem = add("Browser")
                browserItem.setIcon(android.R.drawable.ic_menu_compass)
                browserItem.setOnMenuItemClickListener {
                    PluginApi.openRegisteredTab(id = tabId, tabName = tabName)
                    true
                }
            }
        }
    }

    override fun onMainActivityCreated() { Log.d("Main", "onMainActivityCreated") }
    override fun onMainActivityPaused() { Log.d("Main", "onMainActivityPaused") }
    override fun onMainActivityResumed() { Log.d("Main", "onMainActivityResumed") }
    override fun onMainActivityDestroyed() { Log.d("Main", "onMainActivityDestroyed") }
    override fun onLowMemory() { Log.d("Main", "onLowMemory") }
}
