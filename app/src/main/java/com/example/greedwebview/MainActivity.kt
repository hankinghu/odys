package com.example.greedwebview

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.greedwebview.greed.OdysManager
import com.example.greedwebview.utils.ScrollableWebview
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private var webview: WebView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initNoPre()
    }

    /***
     * 通过预加载的webview进行
     */
    private fun initPre() {
        val webview = OdysManager.fetchCacheWebview(context = baseContext, index = 0)
        Log.d(TAG, "initPre webview $webview")
        webview?.let {
            initWebview(it)
        }
    }

    /**
     * 直接创建webview方式进行
     */
    private fun initNoPre() {
        initWebview(ScrollableWebview(baseContext))
    }

    private fun initWebview(webview: WebView) {
        //添加webview到container中
        val startTime = System.currentTimeMillis()
        webviewContainer.addView(
            webview, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        this.webview = webview
        webview.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                Log.d(TAG, "initWebview view $view title $title")
            }
        }
        webview.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "onPageStarted url $url  time ${System.currentTimeMillis() - startTime}")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "onPageFinished url $url time ${System.currentTimeMillis() - startTime}")

            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.d(
                    TAG,
                    "onReceivedError view $view errorCode $errorCode description $description failingUrl $failingUrl"
                )
            }
        }
        initWebSetting(webview)
        Log.d(TAG, "initWebview webview $webview time  ${System.currentTimeMillis() - startTime}")
        webview.loadUrl("https://www.csdn.net/?orgId=1996123849")

    }

    override fun onDestroy() {
        super.onDestroy()
        //替换掉context并且销毁
        OdysManager.destroyWebView(webview, applicationContext)
    }

    companion object {
        private const val TAG = "GreedWebview"
    }

    private fun initWebSetting(webView: WebView) {
        if (webView.isInEditMode) {
            return
        }
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.isSaveEnabled = true
        val webSettings = webView.settings
        webSettings.allowFileAccess = false
        webSettings.allowFileAccessFromFileURLs = false
        webSettings.allowUniversalAccessFromFileURLs = false
        webSettings.setSupportMultipleWindows(true)
        webSettings.builtInZoomControls = false
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
    }

}