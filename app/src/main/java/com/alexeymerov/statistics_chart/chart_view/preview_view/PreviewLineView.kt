package com.alexeymerov.statistics_chart.chart_view.preview_view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import com.alexeymerov.statistics_chart.App.Companion.MARGIN_2
import com.alexeymerov.statistics_chart.chart_view.AbstractLineView
import com.alexeymerov.statistics_chart.model.ChartLine
import com.alexeymerov.statistics_chart.model.DateItem
import com.alexeymerov.statistics_chart.utils.dpToPxFloat
import java.util.Collections

class PreviewLineView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AbstractLineView(context, attrs, defStyleAttr) {

	override val linePaint: Paint = super.linePaint.apply { strokeWidth = 1.2f.dpToPxFloat() }
	/**
	 * Set new data block
	 */

	override fun setData(newLines: List<ChartLine>, labelsList: List<DateItem>) {
		bottomLabelsList = labelsList
		chartLines = newLines
		updateVerticalMaxValue()
		postInvalidate()
	}

	override fun updateVerticalMaxValue() {
		verticalMaxValue = MIN_VERTICAL_GRID_NUM
		for (chartLine in chartLines) {
			if (!chartLine.isEnabled) continue
			val maxValue = Collections.max(chartLine.dataValues).toFloat()
			if (verticalMaxValue < maxValue) verticalMaxValue = maxValue
		}
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val width = MeasureSpec.getSize(widthMeasureSpec)
		val height = MeasureSpec.getSize(heightMeasureSpec) - MARGIN_2.toInt()
		setMeasuredDimension(width, height)
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		stepX = (width.toFloat() / bottomLabelsList.size.toFloat()) - (MARGIN_2 / bottomLabelsList.size.toFloat())
	}

	/**
	 * Draw block
	 */

	override fun onDraw(canvas: Canvas) = drawLines(canvas)

	override fun drawLines(canvas: Canvas) {
		val yStep = height.toFloat() / getVerticalMaxValue()

		for (chartLineEntry in chartLines) {
			val dataValues = chartLineEntry.dataValues
			if (!chartLineEntry.isEnabled) continue
			for (xIndex in 0 until dataValues.size) {
				val yAxis = (height - dataValues[xIndex] * yStep) + MARGIN_2

				when (xIndex) {
					0 -> linePath.moveTo(MARGIN_2, yAxis)
					else -> linePath.lineTo(MARGIN_2 + xIndex * stepX, yAxis)
				}
			}
			linePaint.color = chartLineEntry.color
			canvas.drawPath(linePath, linePaint)
			linePath.rewind()
		}
	}

	override fun toggleLine(lineIndex: Int) {
		val oldMaxValue = getVerticalMaxValue()
		updateVerticalMaxValue()
		val newMaxValue = getVerticalMaxValue()
		callAnimation(oldMaxValue, newMaxValue)
	}
}