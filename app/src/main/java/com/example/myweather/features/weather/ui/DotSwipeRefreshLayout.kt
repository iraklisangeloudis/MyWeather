package com.example.myweather.features.weather.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.core.view.children
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/** AndroidX owns the gesture and bubble motion; the drawable owns its animated frames. */
class DotSwipeRefreshLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    SwipeRefreshLayout(context, attrs) {
    private var dots: PulsingDotsDrawable? = null
    private var indicator: ImageView? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        // AndroidX 1.1's direct ImageView child is its indicator, separate from screen content.
        indicator = children.filterIsInstance<ImageView>().firstOrNull()
        dots = PulsingDotsDrawable(context).also { indicator?.setImageDrawable(it) }
    }

    override fun setRefreshing(refreshing: Boolean) {
        super.setRefreshing(refreshing)
        updateAnimation()
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        // Gesture refreshes update AndroidX's state internally, without calling setRefreshing.
        if (child === indicator) updateAnimation()
        return super.drawChild(canvas, child, drawingTime)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimation()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateAnimation()
    }

    override fun onDetachedFromWindow() {
        dots?.stop()
        super.onDetachedFromWindow()
    }

    private fun updateAnimation() {
        if (isRefreshing && isAttachedToWindow && windowVisibility == View.VISIBLE) dots?.start()
        else dots?.stop()
    }
}
