package com.example.greedwebview.greed

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.MessageQueue
import android.util.Log
import android.webkit.WebView
import java.lang.ref.SoftReference

/**
 *
 * create time 2022/5/14 4:04 下午
 * create by 胡汉君
 * 用于创建 管理 webview
 */
@SuppressLint("StaticFieldLeak")
object OdysManager {
    //用于存放web的缓存，用软引用来缓存，可以在内存不足时进行回收
    private val cacheWebs = ArrayList<SoftReference<WebView>>()
    private var mainMessageQueue: MessageQueue? = null
    private var size = 0
    private var odysContext: MutableContextWrapper? = null
    const val TAG = "OdysManager"
    fun init(application: Context) {
        odysContext = MutableContextWrapper(application)
        preloadChrome()
    }

    /***
     * 预创建,size 预创建的webview个数
     */
    fun preCreate(size: Int) {
        this.size = size
        createWebview()
    }

    /***
     * 获取缓存中的webview,这里的context必须是activity的context
     */
    fun fetchCacheWebview(context: Context, index: Int): WebView? {
        return if (cacheWebs.isEmpty() || cacheWebs.size <= index) {
            //创建一个新的webview
            Log.d(TAG, "fetchCacheWebview cacheWebs null ")
            WebView(context)
        } else {
            //拿到index的webview，并且移除掉cache中的这个webview，不然其他地方如果又拿了就出问题
            val ref = cacheWebs[index]
            cacheWebs.removeAt(index)
            //移除后再创建一个缓存的webview
            createWebview()
            //这里替换掉webview的context
            val web = ref.get()
            //replace context
            replaceContext(web, context)
            Log.d(TAG, "fetchCacheWebview cacheWebs no null ")
            return web
        }

    }

    private fun replaceContext(webView: WebView?, context: Context) {
        webView?.let {
            val cot = it.context
            if (cot is MutableContextWrapper?) {
                cot.baseContext = context
            }
        }
    }

    /***
     * 创建webview
     */
    private fun createWebview() {
        //如果为null，先初始化
        if (mainMessageQueue != null) {
            mainMessageQueue?.addIdleHandler {
                //看下cacheWebs size中数量,如果cacheWebs的size比preCreate的size少，则创建webview并添加
                Log.d(TAG, "createWebview cacheWebs ${cacheWebs.size} size $size")
                if (cacheWebs.size < size) {
                    odysContext?.let {
                        cacheWebs.add(SoftReference(WebView(it)))
                    }
                }
                false
            }
        } else {
            initMainQueueAndCacheWebView()
        }

    }

    private fun initMainQueueAndCacheWebView() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                mainMessageQueue = Looper.getMainLooper().queue
                createWebview()
            }
            Looper.getMainLooper() == Looper.myLooper() -> {
                mainMessageQueue = Looper.myQueue()
                createWebview()

            }
            else -> {
                Handler(Looper.getMainLooper()).post {
                    mainMessageQueue = Looper.myQueue()
                    createWebview()
                }
            }
        }
    }

    private fun preloadChrome() {
        val webView = odysContext?.let { WebView(it) }
        webView?.destroy()
    }

    //webview销毁时，替换掉activity的context为application的context
    fun destroyWebView(webView: WebView?, context: Context) {
        if (webView != null) {
            webView.stopLoading()
            replaceContext(webView, context)
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }
}