package com.spotifusion.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

/**
 * Spotifusion Android shell.
 *
 * The Android app deliberately hosts the same live Web Player used by the web
 * application. This keeps the UI, fixes, catalog and security changes in sync
 * instead of maintaining a second, stale native UI implementation.
 */
class MainActivity : Activity() {

    companion object {
        private const val MEDIA_PERMISSION_REQUEST = 4101
        private const val FILE_CHOOSER_REQUEST = 4102
        private const val START_URL = "https://spotifusion.vercel.app/"
        private const val APP_HOST = "spotifusion.vercel.app"
        private const val BRIDGE_NAME = "SpotifusionAndroid"
    }

    private var webView: WebView? = null
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0

        val view = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
        }
        webView = view
        setContentView(view)

        configureWebView(view)
        applySystemBarInsets(view)

        val savedWebState = state?.getBundle("webview_state")
        if (savedWebState != null) {
            view.restoreState(savedWebState)
        } else {
            view.loadUrl(START_URL)
        }
    }

    private fun applySystemBarInsets(view: WebView) {
        view.setOnApplyWindowInsetsListener { target, insets ->
            @Suppress("DEPRECATION")
            val top = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.systemBars()).top
            } else {
                insets.systemWindowInsetTop
            }

            @Suppress("DEPRECATION")
            val bottom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.systemBars()).bottom
            } else {
                insets.systemWindowInsetBottom
            }

            target.setPadding(0, top, 0, bottom)
            insets
        }
        view.requestApplyInsets()
    }

    @Suppress("SetJavaScriptEnabled")
    private fun configureWebView(view: WebView) {
        val settings = view.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.loadsImagesAutomatically = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.userAgentString = settings.userAgentString + " SpotifusionAndroid/2.0"

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(view, true)
        }

        view.addJavascriptInterface(AndroidBridge(), BRIDGE_NAME)

        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                val host = uri.host

                if (host.equals(APP_HOST, ignoreCase = true)) {
                    return false
                }

                if (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                        true
                    } catch (_: Exception) {
                        false
                    }
                }

                return false
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    Toast.makeText(
                        this@MainActivity,
                        "Spotifusion could not load. Check your internet connection.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        view.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback

                val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "audio/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }

                return try {
                    startActivityForResult(picker, FILE_CHOOSER_REQUEST)
                    true
                } catch (_: Exception) {
                    fileCallback?.onReceiveValue(null)
                    fileCallback = null
                    Toast.makeText(
                        this@MainActivity,
                        "No audio file picker is available.",
                        Toast.LENGTH_SHORT
                    ).show()
                    false
                }
            }
        }

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    private fun hasMediaPermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED

            else -> true
        }
    }

    private fun requestMediaAccessInternal() {
        if (hasMediaPermission()) {
            notifyWebPermission(true)
            return
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                    MEDIA_PERMISSION_REQUEST
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MEDIA_PERMISSION_REQUEST
                )
            }

            else -> notifyWebPermission(true)
        }
    }

    private fun notifyWebPermission(granted: Boolean) {
        val view = webView ?: return
        val javascript =
            "window.dispatchEvent(new CustomEvent('spotifusion-media-permission'," +
                "{detail:{granted:$granted}}));"
        view.post { view.evaluateJavascript(javascript, null) }
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun requestMediaAccess() {
            runOnUiThread { requestMediaAccessInternal() }
        }

        @JavascriptInterface
        fun hasMediaAccess(): Boolean = hasMediaPermission()
    }

    @Deprecated("Retained for WebView media permission compatibility")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MEDIA_PERMISSION_REQUEST) {
            notifyWebPermission(hasMediaPermission())
        }
    }

    @Deprecated("Retained for WebView file chooser compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != FILE_CHOOSER_REQUEST) return

        val callback = fileCallback ?: return
        fileCallback = null

        val results: Array<Uri>? = if (resultCode == RESULT_OK && data != null) {
            val clipData = data.clipData
            when {
                clipData != null -> Array(clipData.itemCount) { index ->
                    clipData.getItemAt(index).uri
                }

                data.data != null -> arrayOf(data.data!!)
                else -> null
            }
        } else {
            null
        }

        if (results != null) {
            results.forEach { uri ->
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // Some providers do not expose persistable permissions.
                }
            }
        }

        callback.onReceiveValue(results)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val webState = Bundle()
        webView?.saveState(webState)
        outState.putBundle("webview_state", webState)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onDestroy() {
        webView?.let { view ->
            view.stopLoading()
            view.onPause()
            view.setWebChromeClient(null)
            view.webViewClient = WebViewClient()
            view.removeJavascriptInterface(BRIDGE_NAME)
            view.destroy()
        }
        webView = null
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        super.onDestroy()
    }

    @Deprecated("WebView back navigation requires the legacy callback on older Android versions")
    override fun onBackPressed() {
        val view = webView
        if (view != null && view.canGoBack()) {
            view.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
