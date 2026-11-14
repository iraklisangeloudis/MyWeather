package com.example.myweather.features.weather.ui.cards.expandable

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.descendants
import com.example.myweather.R
import kotlin.math.roundToInt

/** Measures the full content independently of progress, so expansion never moves its own target. */
class ExpandableWeatherCard @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val inset = dp(16)
    private val gap = dp(12)
    val compactHeight: Int
    private val compactOffset: Int
    private val summary: Boolean
    private val surface = GradientDrawable().apply { cornerRadius = dp(22).toFloat() }
    var expandedHeight: Int = 0
        private set
    var expansionProgress: Float = 0f
        private set

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ExpandableWeatherCard)
        compactHeight = attributes.getDimensionPixelSize(R.styleable.ExpandableWeatherCard_compactHeight, dp(64))
        compactOffset = attributes.getDimensionPixelSize(R.styleable.ExpandableWeatherCard_compactContentOffset, 0)
        summary = attributes.getBoolean(R.styleable.ExpandableWeatherCard_summaryWhenCompact, false)
        attributes.recycle()
        background = surface
        clipChildren = true
        isFocusable = false
        isClickable = false
    }

    fun setExpansion(progress: Float, staticLayout: Boolean = false) {
        applyProgress(if (staticLayout) 1f else progress)
    }

    private fun applyProgress(value: Float) {
        val progress = value.coerceIn(0f, 1f)
        if (kotlin.math.abs(progress - expansionProgress) < .001f && expandedHeight != 0) return
        expansionProgress = progress
        surface.setColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(context, R.color.weather_surface), (255 * progress).roundToInt()))
        requestLayout()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        require(childCount == 2) { "Weather sections require a heading and content" }
        ViewCompat.setAccessibilityHeading(getChildAt(0), true)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val childWidth = MeasureSpec.makeMeasureSpec((width - inset * 2).coerceAtLeast(0), MeasureSpec.EXACTLY)
        for (index in 0 until childCount) getChildAt(index).measure(childWidth, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        expandedHeight = (inset * 2 + getChildAt(0).measuredHeight + gap + getChildAt(1).measuredHeight).coerceAtLeast(compactHeight)
        setMeasuredDimension(width, (compactHeight + (expandedHeight - compactHeight) * expansionProgress).roundToInt())
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val heading = getChildAt(0)
        val content = getChildAt(1)
        heading.layout(inset, inset, width - inset, inset + heading.measuredHeight)
        val contentTop = inset + heading.measuredHeight + gap
        content.layout(inset, contentTop, width - inset, contentTop + content.measuredHeight)
        heading.alpha = if (summary) expansionProgress else 1f
        heading.importantForAccessibility = if (summary && expansionProgress < .1f) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        content.translationY = if (summary) (dp(12) - compactOffset - contentTop) * (1 - expansionProgress) else 0f
        content.alpha = if (summary) 1f else expansionProgress
        content.importantForAccessibility = if (!summary && expansionProgress < .1f) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        (content as? ViewGroup)?.descendants?.filter { it.tag == "expanding_icon" || it.tag == "expanding_detail" }?.forEach { it.alpha = expansionProgress }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
}
