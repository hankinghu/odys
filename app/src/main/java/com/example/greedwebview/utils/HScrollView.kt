package com.example.greedwebview.utils

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.TranslateAnimation
import androidx.core.widget.NestedScrollView

/**
 *
 * create time 2022/5/15 6:20 下午
 * create by 胡汉君
 */

class HScrollView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    NestedScrollView(context!!, attrs, defStyleAttr) {
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
    private var childView: View? = null

    //childView原来上一次down点击的位置
    private val childRect = Rect()
    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount > 0) {
            childView = getChildAt(0)
            childView?.let {
                childRect.set(it.left, it.top, it.right, it.bottom)
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        Log.d(TAG, "onInterceptTouchEvent event " + ev.action)
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        Log.d(TAG, "onTouchEvent ev " + ev.action)
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startY = ev.y.toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                currentY = ev.y.toInt()
                offset = currentY - lastY
                curOffset = (offset * 0.35).toInt()
                lastY = currentY
                if (currentY != startY && 0 < Math.abs(offset) && Math.abs(offset) < 200) {
                    childView!!.layout(
                        childView!!.left,
                        childView!!.top + curOffset,
                        childView!!.right,
                        childView!!.bottom + curOffset
                    )
                }
            }
            MotionEvent.ACTION_UP -> {
                upDownMoveAnimation()
                childView!!.layout(childRect.left, childRect.top, childRect.right, childRect.bottom)
            }
            else -> {}
        }
        return super.onTouchEvent(ev)
    }

    // 初始化上下回弹的动画效果
    private fun upDownMoveAnimation() {
        val animation = TranslateAnimation(
            0.0f, 0.0f,
            childView!!.top.toFloat(), childRect.top.toFloat()
        )
        animation.duration = 600
        animation.fillAfter = true
        //设置阻尼动画效果
        animation.interpolator = DampInterpolator()
        childView!!.animation = animation
    }

    class DampInterpolator : Interpolator {
        override fun getInterpolation(input: Float): Float {
            //没看过源码，猜测是input是时间（0-1）,返回值应该是进度（0-1）
            //先快后慢，为了更快更慢的效果，多乘了几次，现在这个效果比较满意
            return 1 - (1 - input) * (1 - input) * (1 - input) * (1 - input) * (1 - input)
        }
    }

    companion object {
        private const val TAG = "HScrollView"
    }

    init {
        isFillViewport = true
    }
}