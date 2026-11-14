package com.example.myweather.features.weather.ui.search

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.example.myweather.R
import com.example.myweather.databinding.ActivityMainBinding
import kotlin.math.roundToInt

/** Expands the search button within the header, retaining the location's measured height. */
class ExpandableSearchHeader(private val binding: ActivityMainBinding) {
    var isExpanded = false
        private set
    private var progress = 0f
    private var animator: ValueAnimator? = null
    private val context = binding.root.context
    private val compactWidth = (48 * context.resources.displayMetrics.density).roundToInt()
    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> applyProgress(progress) }

    init {
        binding.weatherHeader.addOnLayoutChangeListener(layoutListener)
    }

    fun setExpanded(expanded: Boolean, animate: Boolean = true) {
        if (isExpanded == expanded && animator == null) return
        isExpanded = expanded
        cancelAnimation()
        binding.searchButton.setImageResource(if (expanded) R.drawable.weather_close_icon else R.drawable.weather_search_icon)
        binding.searchButton.setContentDescription(context.getString(if (expanded) R.string.close_search else R.string.search_city))
        binding.locationHeader.importantForAccessibility = if (expanded)
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        binding.locationHeader.visibility = View.VISIBLE
        binding.searchRow.visibility = View.VISIBLE
        binding.editTextLocation.isFocusableInTouchMode = expanded
        if (!expanded) {
            binding.editTextLocation.clearFocus()
            keyboard().hideSoftInputFromWindow(binding.editTextLocation.windowToken, 0)
        }
        val target = if (expanded) 1f else 0f
        if (!animate || !ValueAnimator.areAnimatorsEnabled() || !binding.weatherHeader.isLaidOut) {
            applyProgress(target)
            completeTransition()
            return
        }
        animator = ValueAnimator.ofFloat(progress, target).apply {
            duration = 280
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { applyProgress(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animator = null
                    completeTransition()
                }
            })
            start()
        }
    }

    private fun applyProgress(value: Float) {
        progress = value
        val header = binding.weatherHeader
        val fullWidth = (header.width - header.paddingLeft - header.paddingRight).coerceAtLeast(compactWidth)
        val width = (compactWidth + (fullWidth - compactWidth) * progress).roundToInt()
        if (binding.searchSurface.layoutParams.width != width) {
            binding.searchSurface.layoutParams = binding.searchSurface.layoutParams.apply { this.width = width }
        }
        binding.locationHeader.alpha = (1f - progress * 2f).coerceIn(0f, 1f)
        binding.searchRow.alpha = ((progress - .4f) / .6f).coerceIn(0f, 1f)
    }

    private fun completeTransition() {
        // INVISIBLE preserves header height, including two-line city names.
        binding.locationHeader.visibility = if (isExpanded) View.INVISIBLE else View.VISIBLE
        binding.searchRow.visibility = if (isExpanded) View.VISIBLE else View.GONE
        if (isExpanded) {
            binding.editTextLocation.requestFocus()
            keyboard().showSoftInput(binding.editTextLocation, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun keyboard() = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    private fun cancelAnimation() {
        animator?.removeAllListeners()
        animator?.removeAllUpdateListeners()
        animator?.cancel()
        animator = null
    }

    fun dispose() {
        cancelAnimation()
        binding.weatherHeader.removeOnLayoutChangeListener(layoutListener)
    }
}
