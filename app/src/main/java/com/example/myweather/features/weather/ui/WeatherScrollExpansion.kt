package com.example.myweather.features.weather.ui

import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import kotlin.math.max

class WeatherScrollExpansion(private val scroll: NestedScrollView, private val content: LinearLayout, private val onPositionChanged: () -> Unit = {}) {
    private val layoutListener = android.view.ViewTreeObserver.OnGlobalLayoutListener { update() }
    private var updating = false

    init {
        scroll.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, _, _, _ -> update() })
        content.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun update() {
        if (updating || scroll.height == 0) return
        updating = true
        try {
            val children = content.children.filter { it.visibility != View.GONE }.toList()
            val fullHeight = content.paddingTop + content.paddingBottom + children.sumOf { child ->
                val margins = child.layoutParams as? ViewGroup.MarginLayoutParams
                (if (child is ExpandableWeatherSection) child.expandedHeight else child.measuredHeight) + (margins?.topMargin ?: 0) + (margins?.bottomMargin ?: 0)
            }
            val staticLayout = !ValueAnimator.areAnimatorsEnabled() ||
                (scroll.context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager).isTouchExplorationEnabled || fullHeight <= scroll.height
            // Reserve the full scroll range so even the last compact row can be scrolled into a card.
            val minimum = max(fullHeight, scroll.height)
            if (content.minimumHeight != minimum) content.minimumHeight = minimum
            var addedHeight = 0
            for (child in children) {
                if (child !is ExpandableWeatherSection) continue
                val compactTop = child.top - addedHeight
                val start = max(16 * scroll.resources.displayMetrics.density, compactTop - scroll.height * .52f)
                val distance = (scroll.height * .24f).coerceIn(96 * scroll.resources.displayMetrics.density, 180 * scroll.resources.displayMetrics.density)
                val raw = ((scroll.scrollY - start) / distance).coerceIn(0f, 1f)
                val progress = raw * raw * (3 - 2 * raw)
                val currentAddedHeight = child.height - child.compactHeight
                child.setExpansion(progress, staticLayout)
                addedHeight += currentAddedHeight
            }
        } finally {
            updating = false
            onPositionChanged()
        }
    }

    fun dispose() {
        if (content.viewTreeObserver.isAlive) content.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        scroll.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
    }
}
