package com.rk.demo

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Message
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.rk.controlpanel.ControlItem
import com.rk.extension.Extension
import com.rk.extension.ExtensionAPI
import com.rk.file_wrapper.FileWrapper
import com.rk.libcommons.child
import com.rk.libcommons.createFileIfNot
import com.rk.libcommons.localDir
import com.rk.libcommons.toast
import com.rk.pluginApi.PluginApi
import com.rk.xededitor.BuildConfig
import com.rk.xededitor.MainActivity.MainActivity
import com.rk.xededitor.MainActivity.tabs.core.CoreFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Main : ExtensionAPI() {

    val tabId = "browser"
    val tabName = "Browser"

    override fun onPluginLoaded(extension: Extension) {
        Log.d("Main", "onPluginLoaded")
        PluginApi.registerTab(id = tabId, tabName = tabName){ tabFragment ->
            object : CoreFragment(){
                var webView: WebView? = null

                override fun getView(): View? {
                    val activity = MainActivity.activityRef.get()!!
                    return WebView(activity).apply {
                        // Enable JavaScript and other settings
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(true)
                        settings.loadsImagesAutomatically = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        //settings.setAppCacheEnabled(true)
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.userAgentString = settings.userAgentString

                        // Enable zoom controls
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        // Enable cookies (optional but useful for full browser behavior)
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        // WebViewClient to handle navigation inside WebView
                        webViewClient = WebViewClient()

                        // WebChromeClient to handle JS alerts, console logs, file chooser, etc.
                        webChromeClient = WebChromeClient()

                        // Download support
                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            val request = DownloadManager.Request(Uri.parse(url))
                            request.setMimeType(mimetype)
                            request.addRequestHeader("User-Agent", userAgent)
                            request.setDescription("Downloading file...")
                            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
                            request.allowScanningByMediaScanner()
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype))
                            val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)
                        }


                        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

                        // Load an initial page or blank
                        loadUrl("https://www.google.com")
                    }.apply { webView = this }

                }

                override fun onDestroy() {
                    webView?.destroy()
                }

                override fun onCreate() {

                }

                override fun onClosed() {
                    onDestroy()
                }

            }
        }


//        PluginApi.registerControlItem(id = tabId, item = ControlItem(label = tabName, description = null, sideEffect = {
//            PluginApi.openRegisteredTab(id = tabId, tabName = tabName)
//        }))

        MainActivity.activityRef.get()?.menu?.apply {
            val browserItem = add("Browser")
            browserItem.setIcon(android.R.drawable.ic_menu_compass)
            browserItem.setOnMenuItemClickListener {
                PluginApi.openRegisteredTab(id = tabId, tabName = tabName)
                true
            }
        }

    }

    override fun onMainActivityCreated() {
        Log.d("Main", "onMainActivityCreated")
    }

    override fun onMainActivityPaused() {
        Log.d("Main", "onMainActivityPaused")
    }

    override fun onMainActivityResumed() {
        Log.d("Main", "onMainActivityResumed")
    }

    override fun onMainActivityDestroyed() {
        Log.d("Main", "onMainActivityDestroyed")
    }

    override fun onLowMemory() {
        Log.d("Main", "onLowMemory")
    }

}