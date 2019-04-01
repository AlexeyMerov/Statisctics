package com.alexeymerov.statistics_chart.chart_view.preview_view

import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import com.alexeymerov.statistics_chart.chart_view.AbstractLineView
import com.alexeymerov.statistics_chart.model.ChartLine
import com.alexeymerov.statistics_chart.utils.dpToPxFloat
import java.util.*

class PreviewLineView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AbstractLineView(context, attrs, defStyleAttr) {

	private val LINES_PROPERTY_NAME = "lines"
	private val MARGIN = 3.dpToPxFloat()

	override var bottomLabelsList = listOf<String>()

	/**
	 * Set new data block
	 */

	override fun setData(newLines: List<ChartLine>, labelsList: List<String>) {
		bottomLabelsList = labelsList
		chartLines = newLines
		postInvalidate()
	}

	override fun getVerticalMaxValue(): Int {
		if (needAnimateValues) return valueAnimator.getAnimatedValue(LINES_PROPERTY_NAME) as Int

		if (vertical == 0) {
			vertical = MIN_VERTICAL_GRID_NUM
			chartLines
				.takeIf { !it.isEmpty() }
				?.asSequence()
				?.filter { it.isEnabled }
				?.map { it.dataValues }
				?.map { Collections.max(it) }
				?.filter { vertical < it }
				?.forEach { vertical = it }
		}

		return vertical
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		stepX = (width.toFloat() / bottomLabelsList.size.toFloat()) - (MARGIN / bottomLabelsList.size.toFloat())
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val width = MeasureSpec.getSize(widthMeasureSpec)
		val height = MeasureSpec.getSize(heightMeasureSpec) - MARGIN.toInt()
		setMeasuredDimension(width, height)
	}

	/**
	 * Draw block
	 */

	override fun onDraw(canvas: Canvas) = drawLines(canvas)

	override fun drawLines(canvas: Canvas) {
		val yStep = height / getVerticalMaxValue().toFloat()

		for (chartLineEntry in chartLines) {
			val dataValues = chartLineEntry.dataValues
			if (!chartLineEntry.isEnabled) continue
			for (xIndex in 0 until dataValues.size) {
				val yAxis = (height - dataValues[xIndex] * yStep) + MARGIN

				when (xIndex) {
					0 -> linePath.moveTo(MARGIN, yAxis)
					else -> linePath.lineTo(MARGIN + xIndex * stepX, yAxis)
				}
			}
			linePaint.color = chartLineEntry.color
			canvas.drawPath(linePath, linePaint)
			linePath.rewind()
		}
	}

	override fun toggleLine(lineIndex: Int) {
		val oldMaxValue = vertical
		resetVerticalMaxNum()
		val newMaxValue = getVerticalMaxValue()
		callAnimation(oldMaxValue, newMaxValue)
	}

	private fun callAnimation(oldMaxValue: Int, newMaxValue: Int) {
		valueAnimator.setValues(PropertyValuesHolder.ofInt(LINES_PROPERTY_NAME, oldMaxValue, newMaxValue))
		valueAnimator.start()
	}
}