package com.example.greedwebview.utils

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.animation.Interpolator
import android.view.animation.TranslateAnimation
import android.webkit.WebView
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/**
 *
 * create time 2022/5/15 6:36 下午
 * create by 胡汉君
 * 带有弹性动画的webview
 * 通过拦截touch event 来添加弹性webview
 *
 */
class ScrollableWebview @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : WebView(context, attrs) {
    //Y轴本次点击的位置
    private var currentY = 0

    //Y轴本次down点击的位置
    private var startY = 0

    //Y轴上次move事件点击的位置
    private var lastY = 0

    //Y轴上两次move事件之间的偏移量
    private var offset = 0

    //Y轴上两次move事件之间的偏移量*系数
    private var curOffset = 0
    private val scrollFactor = 0.35f
    private val scrollIv = 200
    private var canScroll = false
    private val rect = Rect()

    //是否正在scrolled中
    private var isScrolled = false
    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
        //这个方法在webview内容滑动到顶部和底部时调用
        log("onOverScrolled scrollX $scrollX scrollY $scrollY clampedX $clampedX clampedY $clampedY")
        canScroll = clampedY
    }

    companion object {
        private const val TAG = "ScrollableWebview"
    }

    private fun log(message: String) {
        Log.d(TAG, message)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        Log.d(TAG, "onFinishInflate left $left top $top  right $right bottom $bottom")
        rect.set(left, top, right, bottom)

    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        //拦截这个方法
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startY = ev.y.toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                if (canScroll) {
                    currentY = ev.y.toInt()
                    offset = currentY - lastY
                    curOffset = (offset * scrollFactor).toInt()
                    lastY = currentY
                    if (currentY != startY && 0 < abs(offset) && abs(offset) < scrollIv) {
                        isScrolled = true
                        layout(
                            left,
                            top + curOffset,
                            right,
                            bottom + curOffset
                        )
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isScrolled) {
                    upDownMoveAnimation()
                    layout(rect.left, rect.top, rect.right, rect.bottom)
                }
            }
            else -> {}
        }

        return super.onTouchEvent(ev)
    }

    // 初始化上下回弹的动画效果
    private fun upDownMoveAnimation() {
        val animation = TranslateAnimation(
            0.0f, 0.0f,
            top.toFloat(), rect.top.toFloat()
        )
        animation.duration = 800
        animation.fillAfter = true
        //设置阻尼动画效果
        animation.interpolator = SpringInterpolation()
        this.animation = animation
    }

    class SpringInterpolation : Interpolator {
        override fun getInterpolation(input: Float): Float {
            val minus10 = -10
            val int4 = 4
            val factor=4
            return (2.0.pow((minus10 * input).toDouble()) * sin((input - factor / int4) * (2 * Math.PI) / factor) + 1).toFloat()
        }
    }
}