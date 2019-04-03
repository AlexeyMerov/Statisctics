package com.alexeymerov.statistics_chart.chart_view

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.alexeymerov.statistics_chart.model.ChartLine
import com.alexeymerov.statistics_chart.model.DateItem
import com.alexeymerov.statistics_chart.utils.dpToPxFloat

abstract class AbstractLineView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
														  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

	protected val MIN_VERTICAL_GRID_NUM = 4

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

	protected var stepX = 0f

	protected open val linePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeWidth = 2.dpToPxFloat()
		strokeCap = Paint.Cap.ROUND
		strokeJoin = Paint.Join.ROUND
	}

	protected val valueAnimator = ValueAnimator().apply {
		duration = 500
		interpolator = AccelerateDecelerateInterpolator()
		repeatMode = ValueAnimator.RESTART
		addUpdateListener { postInvalidate() }
		addListener(object : Animator.AnimatorListener {
			override fun onAnimationStart(animation: Animator?) {
				needAnimateValues = true
			}

			override fun onAnimationEnd(animation: Animator?) {
				needAnimateValues = false
			}

			override fun onAnimationRepeat(animation: Animator?) {}

			override fun onAnimationCancel(animation: Animator?) {}
		})
	}

	protected var bottomLabelsList = listOf<DateItem>()

	protected abstract fun drawLines(canvas: Canvas)

	internal abstract fun getVerticalMaxValue(): Int

	protected fun resetVerticalMaxNum() {
		vertical = 0
	}

	abstract fun setData(newLines: List<ChartLine>, labelsList: List<DateItem>)

	abstract fun toggleLine(lineIndex: Int)
}