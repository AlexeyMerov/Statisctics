package com.alexeymerov.statistics_chart.chart_view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import com.alexeymerov.statistics_chart.THEME_SHARED_KEY
import com.alexeymerov.statistics_chart.model.ChartLine
import com.alexeymerov.statistics_chart.utils.SPHelper
import com.alexeymerov.statistics_chart.utils.dpToPxFloat

abstract class AbstractLineView(context: Context, attrs: AttributeSet?, defStyleAttr: Int
) : View(context, attrs, defStyleAttr) {

	protected companion object {
		const val MIN_VERTICAL_GRID_NUM = 4
	}

	protected var isLightThemeEnabled = SPHelper.getShared(THEME_SHARED_KEY, true)

	protected var xValuesToDisplay = 24
		get() {
			if (field > bottomLabelsList.size) field = bottomLabelsList.size
			return field
		}
	protected var startIndex = 0

	protected var vertical = 0

	protected var chartLines = listOf<ChartLine>()

	protected val linePath = Path()

	protected var needAnimateValues = false

	protected val linePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeWidth = 2.dpToPxFloat()
		strokeCap = Paint.Cap.ROUND
		strokeJoin = Paint.Join.ROUND
	}

	protected val valueAnimator = ValueAnimator().apply {
		duration = 500
		interpolator = AccelerateDecelerateInterpolator()
		repeatMode = ValueAnimator.RESTART
		doOnStart { needAnimateValues = true }
		doOnEnd { needAnimateValues = false }
		addUpdateListener { postInvalidate() }
	}

	protected abstract var bottomLabelsList: List<String>

	var stepX = 0f

	protected abstract fun drawLines(canvas: Canvas)

	protected abstract fun getVerticalMaxValue(): Int

	protected fun resetVerticalMaxNum() {
		vertical = 0
	}

	abstract fun setData(newLines: List<ChartLine>, labelsList: List<String>)

	abstract fun toggleLine(lineIndex: Int)
}