package com.example.myweather.features.weather.ui.background

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.example.myweather.R
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Draws one adaptive living sky behind the existing UI, with no images or network requests. */
class WeatherBackgroundLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {
    private val density = resources.displayMetrics.density
    private val systemNight = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    private var scene = WeatherSkyScene.fromWeather(null, null, systemNight)
    private val baseColor = ContextCompat.getColor(context, R.color.weather_canvas)
    var skyColor: Int = ContextCompat.getColor(context, R.color.weather_sky)
        private set
    var onSkyColorChanged: ((Int) -> Unit)? = null
    internal val isMotionRunning: Boolean get() = motion?.isStarted == true
    private var motionAllowed = false
    private var motion: ValueAnimator? = null
    private var paletteTransition: ValueAnimator? = null
    private var phase = 0f
    private var weatherArea = 0f
    private val backgroundPaint = Paint()
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cloud = Path().apply {
        moveTo(0f, 36f)
        cubicTo(0f, 24f, 10f, 17f, 23f, 19f)
        cubicTo(26f, 2f, 48f, -4f, 61f, 10f)
        cubicTo(78f, -8f, 106f, 2f, 108f, 23f)
        cubicTo(128f, 19f, 141f, 30f, 140f, 42f)
        cubicTo(138f, 51f, 130f, 54f, 117f, 54f)
        lineTo(22f, 54f)
        cubicTo(8f, 54f, 0f, 48f, 0f, 36f)
        close()
    }

    init { setWillNotDraw(false) }

    fun setWeather(code: Int?, isDay: Int?) {
        val next = WeatherSkyScene.fromWeather(code, isDay, systemNight)
        if (next == scene) return
        val previousWasWeather = scene.condition != SkyCondition.STILL
        scene = next
        updateGlow()
        val target = topColor(next)
        paletteTransition?.cancel()
        if (previousWasWeather && motionAllowed && isAttachedToWindow && ValueAnimator.areAnimatorsEnabled()) {
            val from = skyColor
            paletteTransition = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 850
                addUpdateListener { applySkyColor(ColorUtils.blendARGB(from, target, it.animatedValue as Float)) }
                start()
            }
        } else applySkyColor(target)
        updateMotion()
        invalidate()
    }

    fun setWeatherArea(bottom: Float) {
        val area = bottom.coerceIn(0f, height.toFloat())
        if (kotlin.math.abs(area - weatherArea) < 1f) return
        weatherArea = area
        updateMotion()
        invalidate()
    }

    fun setMotionAllowed(allowed: Boolean) {
        motionAllowed = allowed
        if (!allowed && paletteTransition != null) {
            paletteTransition?.cancel()
            paletteTransition = null
            applySkyColor(topColor(scene))
        }
        updateMotion()
    }

    private fun topColor(value: WeatherSkyScene): Int = if (value.isNight) when (value.condition) {
        SkyCondition.CLEAR -> Color.rgb(21, 38, 64)
        SkyCondition.CLOUDY -> Color.rgb(28, 43, 64)
        SkyCondition.FOG -> Color.rgb(34, 49, 67)
        SkyCondition.RAIN -> Color.rgb(25, 43, 64)
        SkyCondition.SNOW -> Color.rgb(29, 45, 65)
        SkyCondition.STORM -> Color.rgb(21, 33, 51)
        SkyCondition.STILL -> ContextCompat.getColor(context, R.color.weather_sky)
    } else when (value.condition) {
        SkyCondition.CLEAR -> Color.rgb(218, 237, 248)
        SkyCondition.CLOUDY -> Color.rgb(211, 226, 237)
        SkyCondition.FOG -> Color.rgb(222, 231, 236)
        SkyCondition.RAIN -> Color.rgb(199, 216, 230)
        SkyCondition.SNOW -> Color.rgb(228, 239, 247)
        SkyCondition.STORM -> Color.rgb(183, 203, 220)
        SkyCondition.STILL -> ContextCompat.getColor(context, R.color.weather_sky)
    }

    private fun applySkyColor(color: Int) {
        skyColor = color
        updateGradient()
        onSkyColorChanged?.invoke(color)
        invalidate()
    }

    private fun updateGradient() {
        backgroundPaint.shader = LinearGradient(0f, 0f, 0f, max(height, 1).toFloat(), skyColor, baseColor, Shader.TileMode.CLAMP)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGradient()
        updateGlow()
    }

    private fun updateGlow() {
        glowPaint.shader = RadialGradient(0f, 0f, max(width * .55f, 1f),
            if (scene.isNight) Color.rgb(158, 188, 224) else Color.rgb(250, 213, 142), Color.TRANSPARENT, Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        if (scene.condition == SkyCondition.STILL || weatherArea <= 0f) return
        val area = min(weatherArea, height * .58f)
        val checkpoint = canvas.save()
        canvas.clipRect(0f, 0f, width.toFloat(), area)
        if (scene.condition == SkyCondition.CLEAR) {
            glowPaint.alpha = if (scene.isNight) 16 else 38
            canvas.save()
            canvas.translate(width * .88f + sin(phase * PI.toFloat() * 2) * 8 * density, area * .15f)
            canvas.drawCircle(0f, 0f, width * .55f, glowPaint)
            canvas.restore()
            if (scene.isNight) drawStars(canvas, area)
        }
        val count = when (scene.condition) {
            SkyCondition.CLEAR -> if (scene.cloudCover > 0f) 1 else 0
            SkyCondition.STORM -> 4
            SkyCondition.FOG -> 0
            else -> 3
        }
        cloudPaint.color = if (scene.isNight) Color.rgb(174, 195, 218) else Color.WHITE
        for (index in 0 until count) {
            val cloudWidth = width * (.55f + index * .08f)
            val x = ((phase * (index + 1) + index * .37f) % 1f) * (width + cloudWidth) - cloudWidth
            val y = area * (.15f + index * .18f)
            cloudPaint.alpha = ((if (scene.isNight) 21 else 62) * fade(y, area) * (.55f + scene.cloudCover)).toInt()
            canvas.save()
            canvas.translate(x, y)
            canvas.scale(cloudWidth / 140f, cloudWidth / 140f)
            canvas.drawPath(cloud, cloudPaint)
            canvas.restore()
        }
        when (scene.condition) {
            SkyCondition.RAIN, SkyCondition.STORM -> drawPrecipitation(canvas, area, false)
            SkyCondition.SNOW -> drawPrecipitation(canvas, area, true)
            SkyCondition.FOG -> drawFog(canvas, area)
            else -> Unit
        }
        canvas.restoreToCount(checkpoint)
    }

    private fun fade(y: Float, area: Float): Float = (1f - y / max(area, 1f)).coerceIn(0f, 1f).let { it * it }

    private fun drawStars(canvas: Canvas, area: Float) {
        particlePaint.color = Color.rgb(211, 228, 247)
        for (index in 0 until 18) {
            val x = (((index * 47 + 13) % 97) / 97f) * width
            val y = (((index * 31 + 7) % 83) / 100f) * area
            val twinkle = .65f + .35f * sin((phase * (6 + index % 4) * PI * 2).toFloat())
            particlePaint.alpha = (72 * twinkle * fade(y, area)).toInt()
            canvas.drawCircle(x, y, (.65f + (index % 3) * .2f) * density, particlePaint)
        }
    }

    private fun drawPrecipitation(canvas: Canvas, area: Float, snow: Boolean) {
        particlePaint.color = if (scene.isNight) Color.rgb(211, 228, 246) else Color.rgb(116, 149, 173)
        particlePaint.strokeWidth = density
        for (index in 0 until (if (snow) 24 else 20)) {
            val travel = (phase * (if (snow) 5 + index % 3 else 80 + index % 5) + index * .137f) % 1f
            val y = travel * (area + 20 * density) - 10 * density
            val drift = sin((phase * 8 * PI + index).toFloat()) * (if (snow) 9 else 2) * density
            val x = (((index * 43 + 19) % 101) / 101f) * width + drift
            particlePaint.alpha = ((if (snow) 100 else 44) * fade(y, area)).toInt()
            if (snow) canvas.drawCircle(x, y, (1f + index % 3 * .35f) * density, particlePaint)
            else canvas.drawLine(x, y, x - 2 * density, y + 9 * density, particlePaint)
        }
    }

    private fun drawFog(canvas: Canvas, area: Float) {
        cloudPaint.color = if (scene.isNight) Color.rgb(163, 184, 207) else Color.WHITE
        for (index in 0..2) {
            val y = area * (.24f + index * .17f)
            cloudPaint.alpha = (30 * fade(y, area)).toInt()
            val drift = sin((phase * 4 * PI + index).toFloat()) * 18 * density
            canvas.drawOval(-width * .3f + drift, y, width * 1.3f + drift, y + 25 * density, cloudPaint)
        }
    }

    private fun updateMotion() {
        val shouldRun = motionAllowed && weatherArea > 0f && scene.condition != SkyCondition.STILL &&
            isAttachedToWindow && windowVisibility == View.VISIBLE && ValueAnimator.areAnimatorsEnabled()
        if (shouldRun && motion == null) {
            val savedPhase = phase
            motion = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 120_000
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { phase = it.animatedValue as Float; invalidate() }
                start()
                currentPlayTime = (savedPhase * duration).toLong()
            }
        } else if (!shouldRun) stopMotion()
    }

    private fun stopMotion() {
        motion?.cancel()
        motion = null
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); updateMotion() }
    override fun onWindowVisibilityChanged(visibility: Int) { super.onWindowVisibilityChanged(visibility); updateMotion() }
    override fun onDetachedFromWindow() { stopMotion(); paletteTransition?.cancel(); super.onDetachedFromWindow() }

    fun dispose() {
        stopMotion()
        paletteTransition?.cancel()
        paletteTransition = null
        onSkyColorChanged = null
    }
}
