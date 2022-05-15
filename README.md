# odys

## 前言
如下图打开一个WebView通常会经历以下几个阶段：
![在这里插入图片描述](https://img-blog.csdnimg.cn/953e81e16d0b4de69c1e9d20736d4b8b.png)
上图中webview初始化阶段，对于用户来说是无反馈。当App首次打开时，默认是并不初始化浏览器内核的，只有当创建WebView实例的时候，才会创建WebView的基础框架。App中打开WebView的第一步并不是建立连接，而是启动浏览器内核。

## webview初始化时间数据对比
| 首次打开时间 |二次打开时间  |
|--|--|
| 498ms | 142ms |

可以看出webview首次打开时间比二次打开时间3倍还多，初次打开webview时间长的原因是首次打开webview的时候要加载webview的内核。

## webview预创建
由上面的对比可知，首次打开webview的时间是很长的，如果在业务打开webview的时候再创建webview加载url，打开的速度会比较慢。
- 可以在app初始化后，创建一个webview的缓存池，当业务需要使用webview的时候从缓存池中获取webview，由于缓存池中的webview是已经初始化了的，所以业务打开webview相当于二次打开webview。

 - 由于webview只能在主线程中创建，可以在为了不影响冷启动性能，可以使用idleHandler方法在主线程空闲时创建webview
 
**1、创建webview并缓存**
```java
    fun preCreate(size: Int, application: Context) {
        this.size = size
        odysContext = MutableContextWrapper(application)
         createWebview()
    }
    mainMessageQueue?.addIdleHandler {
            //看下cacheWebs size中数量,如果cacheWebs的size比preCreate的size少，则创建webview并添加
            if (cacheWebs.size < size) {
                odysContext?.let {
                    cacheWebs.add(SoftReference(WebView(it)))
                }
            }
            false
        }

```
- 上面cacheWebs中使用软引用来来存放webview的实例，这样在内存不足时可以进行释放，添加webview到缓存放在idleHandler中。
- 上面对context使用MutableContextWrapper进行一层包装，MutableContextWrapper可以替换context，因为预创建webview的时候不能使用activity的context（预创建的webview不属于任何activity，如果使用activity会内存泄漏），这里使用的是applicationContext进行webview初始化。
- MutableContextWrapper代码如下，提供了setBaseContext方法来设置context，这样在activity中获取webview时就可以进行替换了。
```java
public class MutableContextWrapper extends ContextWrapper {
    public MutableContextWrapper(Context base) {
        super(base);
    }
    
    /**
     * Change the base context for this ContextWrapper. All calls will then be
     * delegated to the base context.  Unlike ContextWrapper, the base context
     * can be changed even after one is already set.
     * 
     * @param base The new base context for this wrapper.
     */
    public void setBaseContext(Context base) {
        mBase = base;
    }
}
```
**2、获取webview实例**
在activity中获取webview实例，这里activity一般定义为webviewActivity，如下

```java
    /***
     * 获取缓存中的webview,这里的context必须是activity的context
     */
    fun fetchCacheWebview(context: Context, index: Int): WebView? {
        return if (cacheWebs.isEmpty() || cacheWebs.size <= index) {
            //创建一个新的webview
            WebView(context)
        } else {
            //拿到index的webview，并且移除掉cache中的这个webview，不然其他地方如果又拿了就出问题
            val ref = cacheWebs[index]
            cacheWebs.removeAt(index)
               //移除后再创建一个缓存的webview,保证缓存池中的webview数量不变
            createWebview()
            //这里替换掉webview的context
            val web = ref.get()
            //replace context
            replaceContext(web, context)
            return web
        }

    }
```
注意获取cacheWebs中的activity时需要activity的context，并且使用`replaceContext(web,context)` 方法替换掉原来webview中的application context

```java
    private fun replaceContext(webView: WebView?, context: Context) {
        webView?.let {
            val cot = it.context
            if (cot is MutableContextWrapper?) {
                cot.baseContext = context
            }
        }
    }
```
- 在获取到cacheWebs中的webview实例后，要将这个实例从cacheWebs中移除，防止其他地方再获取这个实例。
-  在移除cacheWebs中的webview实例后，要再往缓存中加webview实例，保证缓存池中webview的缓存数量。

**3、activity中如何使用预创建的webview**
正常来说webview的使用方式有在xml中定义webview，和动态添加webview。

1、往布局里面加WebView

```java
<WebView
    android:id="@+id/webview"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
/>
```
2、动态添加webview

```java

val webView = WebView(activityContext)
setContentView(webView)
```
- 由于要使用预创建的webview，所以直接在xml布局中添加webview的话就无法使用预创建的webview，因为xml中的元素解析生成对象是在`setContentView()`方法中，会在xml解析时自动生成webview对象。
- 如果想使用xml的方式并且使用预加载的webview，可以用下面的方式：

```xml
    <FrameLayout
        android:id="@+id/webviewContainer"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />
```
然后在代码中进行如下设置：

```java
	   //先获取预创建的webview
	   val webview=OdysManager.fetchCacheWebview(context,0)
	   //将webview 添加到webviewContainer中
       webviewContainer.addView(
            webview, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
```
动态添加预创建的webview

```java

 //先获取预创建的webview
	   val webview=OdysManager.fetchCacheWebview(context,0)
	   //将webview 添加到webviewContainer中
	   setContentView(webview)
```
3、销毁webview
在activity的onDestory中对webview进行销毁，销毁webview时先替换掉webview的context为applicationContext如下：

```java
    override fun onDestroy() {
        super.onDestroy()
        //替换掉context并且销毁
        if (webView != null) {
            webView.stopLoading()
            replaceContext(webView, context)
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }
```
## 数据对比
以创建webview之前开始，到onPageStarted的时间戳统计数据如下



- 1、不使用预创建webview

|  机型|  时间(ms)|
|--|--|
|Honor 20 |  648|
|Honor 20  |  624|
|  Honor 20|  593|
|Honor 20 |  565|
|Honor 20 |  515|

2、使用预创建webview

|  机型|  时间(ms)|
|--|--|
|Honor 20 |  350|
|Honor 20  |  297|
|  Honor 20|  310|
|Honor 20 |  323|
|Honor 20 |  254|

## 参考
[1、https://tech.meituan.com/2017/06/09/webviewperf.html](https://tech.meituan.com/2017/06/09/webviewperf.html)
