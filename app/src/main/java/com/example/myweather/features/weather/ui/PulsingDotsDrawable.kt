package com.example.myweather.features.weather.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.example.myweather.R
import kotlin.math.max
import kotlin.math.sin

internal class PulsingDotsDrawable(context: Context) : Drawable(), Animatable {
    private val density = context.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.weather_accent)
    }
    private var animator: ValueAnimator? = null
    private var phase = 0f
    private var opacity = 255

    override fun start() {
        if (animator != null || !ValueAnimator.areAnimatorsEnabled()) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                // Invalidate the drawable itself so the ImageView's hardware layer redraws.
                invalidateSelf()
            }
            start()
        }
    }

    override fun stop() {
        if (animator == null) return
        animator?.cancel()
        animator = null
        phase = 0f
        invalidateSelf()
    }

    override fun isRunning() = animator?.isRunning == true
    override fun getIntrinsicWidth() = (40 * density).toInt()
    override fun getIntrinsicHeight() = intrinsicWidth

    override fun draw(canvas: Canvas) {
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        for (index in 0..2) {
            val pulse = if (isRunning) max(0f, sin((phase * Math.PI * 2 - index * .85).toFloat())) else .5f
            paint.alpha = (opacity * (.35f + .65f * pulse)).toInt()
            val radius = (2.3f + .5f * pulse) * density
            canvas.drawCircle(cx + (index - 1) * 8f * density,
                cy - pulse * 2.5f * density, radius, paint)
        }
    }

    override fun setAlpha(alpha: Int) { opacity = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Drawable opacity is no longer used")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
