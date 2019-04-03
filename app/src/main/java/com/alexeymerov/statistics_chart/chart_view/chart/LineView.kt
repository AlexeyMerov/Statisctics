package com.alexeymerov.statistics_chart.chart_view.chart

import android.animation.ArgbEvaluator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.alexeymerov.statistics_chart.App
import com.alexeymerov.statistics_chart.App.Companion.MARGIN_4
import com.alexeymerov.statistics_chart.App.Companion.MARGIN_6
import com.alexeymerov.statistics_chart.App.Companion.MARGIN_8
import com.alexeymerov.statistics_chart.R
import com.alexeymerov.statistics_chart.chart_view.AbstractLineView
import com.alexeymerov.statistics_chart.interfaces.UpdatableTheme
import com.alexeymerov.statistics_chart.model.ChartLine
import com.alexeymerov.statistics_chart.model.DateItem
import com.alexeymerov.statistics_chart.model.Popup
import com.alexeymerov.statistics_chart.utils.SPHelper
import com.alexeymerov.statistics_chart.utils.dpToPx
import com.alexeymerov.statistics_chart.utils.dpToPxFloat
import com.alexeymerov.statistics_chart.utils.formatK
import com.alexeymerov.statistics_chart.utils.formatM
import com.alexeymerov.statistics_chart.utils.spToPxFloat
import java.util.Collections

class LineView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AbstractLineView(context, attrs, defStyleAttr), UpdatableTheme {

	override var isLightThemeEnabled = SPHelper.getShared(App.THEME_SHARED_KEY, true)

	private val popupHandler = PopupHandler(this)

	private val POPUP_SHADOW_PROPERTY = "popupShadowRectPaint"
	private val POPUP_PROPERTY = "popupRectPaint"
	private val SMALL_DOT_PROPERTY = "smallDotPaint"
	private val TEXT_PROPERTY = "textPaint"
	private val BACKGROUND_LINE_PROPERTY = "backgroundLinePaint"
	private val LINES_PROPERTY = "lines"

	private val BOTTOM_LABELS_TOP_MARGIN = MARGIN_6
	private val LEFT_LABEL_BOTTOM_MARGIN = MARGIN_4

	private var needUpdatePreview = true

	private val leftLabelsList = mutableListOf<String>()

	private var leftLabelsModule = 0
	private var bottomTextHeight = 0
	private var bottomTextDescent = 0

	private var leftPadding = 45.dpToPx() / 3 * 2
	private var longestWidth = 0

	private val bottomTextRect = Rect()

	private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		updatePaint(context, this, R.color.labels_text_light, R.color.labels_text_dark)
		textSize = 12.spToPxFloat()
		textAlign = Paint.Align.LEFT
		style = Paint.Style.FILL
		typeface = Typeface.DEFAULT
	}

	private val backgroundLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		updatePaint(context, this, R.color.lines_light, R.color.lines_dark)
		style = Paint.Style.STROKE
		strokeWidth = 0.5f.dpToPxFloat()
	}

	private val colorAnimation = ValueAnimator().apply {
		duration = 250
		repeatMode = ValueAnimator.RESTART
		addUpdateListener { animator ->
			popupHandler.updateShadowColor(animator.getAnimatedValue(POPUP_SHADOW_PROPERTY) as Int)
			popupHandler.updateRectColor(animator.getAnimatedValue(POPUP_PROPERTY) as Int)
			popupHandler.updateSmallDotColor(animator.getAnimatedValue(SMALL_DOT_PROPERTY) as Int)
			textPaint.color = animator.getAnimatedValue(TEXT_PROPERTY) as Int
			backgroundLinePaint.color = animator.getAnimatedValue(BACKGROUND_LINE_PROPERTY) as Int
			postInvalidate()
		}
	}

	var onDataLoaded: () -> Unit = {}

	override fun getVerticalMaxValue(): Int {
		if (needAnimateValues) return valueAnimator.getAnimatedValue(LINES_PROPERTY) as Int

		vertical = MIN_VERTICAL_GRID_NUM
		for (chartLine in chartLines) {
			if (!chartLine.isEnabled) continue

			val dataValues = chartLine.dataValues

			var startIndex = startIndex
			var toIndex = startIndex + xValuesToDisplay
			if (toIndex >= dataValues.size) toIndex = dataValues.size - 1

			if (startIndex < 0) startIndex = 0
			else if (startIndex >= toIndex) startIndex = toIndex - xValuesToDisplay

			val subList = dataValues.subList(startIndex, toIndex)
			val maxValue = Collections.max(subList)
			if (vertical < maxValue) vertical = maxValue
		}

		return vertical
	}

	/**
	 * Set new data block
	 */

	override fun setData(newLines: List<ChartLine>, labelsList: List<DateItem>) {
		needUpdatePreview = true
		bottomLabelsList = labelsList
		setBottomTextList()
		chartLines = newLines
		updateLeftLabelsList()
		postInvalidate()
	}

	private fun updateLeftLabelsList() {
		leftLabelsList.clear()

		val verticalGridNum = getVerticalMaxValue()

		leftLabelsModule = 1
		if (verticalGridNum >= 10) leftLabelsModule = (verticalGridNum / 10) * 2

		repeat(verticalGridNum) {
			if (leftLabelsModule == 1 || it % leftLabelsModule == 0 || it == 0) {
				val resultValue = it - it % leftLabelsModule
				leftLabelsList.add(resultValue.toString())
			}
		}
	}

	private fun setBottomTextList() {
		longestWidth = 0
		bottomTextDescent = 0

		for (s in bottomLabelsList) {
			textPaint.getTextBounds(s.shortDate, 0, s.shortDate.length, bottomTextRect)
			val height = bottomTextRect.height()
			if (bottomTextHeight < height) bottomTextHeight = height

			val width = bottomTextRect.width()
			if (longestWidth < width) longestWidth = width

			val abs = Math.abs(bottomTextRect.bottom)
			if (bottomTextDescent < abs) bottomTextDescent = abs
		}

		if (leftPadding < longestWidth / 2) leftPadding = longestWidth / 2
	}

	/**
	 * Draw block
	 */

	override fun onDraw(canvas: Canvas) {
		drawLines(canvas)
		drawLeftLabelsWithLines(canvas)
		drawBottomLabels(canvas)
		popupHandler.drawPopup(canvas)
	}

	override fun drawLines(canvas: Canvas) {
		val heightFloat = getHeightWithMargins()
		val yStep = heightFloat / getVerticalMaxValue().toFloat()

		for (chartLineEntry in chartLines) {
			if (!chartLineEntry.isEnabled) continue
			val dataValues = chartLineEntry.dataValues
			for (xIndex in 0 until xValuesToDisplay) {
				val dataIndex = xIndex + startIndex
				if (dataIndex >= dataValues.size) continue
				val yAxis = heightFloat - (dataValues[dataIndex].toFloat() * yStep)
				when (xIndex) {
					0 -> linePath.moveTo(0f, yAxis)
					else -> linePath.lineTo(xIndex * stepX, yAxis)
				}
			}
			linePaint.color = chartLineEntry.color
			canvas.drawPath(linePath, linePaint)
			linePath.rewind()
		}
	}

	private fun drawLeftLabelsWithLines(canvas: Canvas) {
		val currentMaxValue = getVerticalMaxValue()
		val yStep = getHeightWithMargins() / currentMaxValue.toFloat()
		val stopX = width.toFloat()

		for (i in 0 until currentMaxValue step leftLabelsModule) {
			val yAxisValue = (i * yStep)
			val value = currentMaxValue - i
			val valueString = when {
				value > 1000000 -> value.formatM()
				value > 1000 -> value.formatK()
				else -> value.toString()
			}

			canvas.drawLine(0f, yAxisValue, stopX, yAxisValue, backgroundLinePaint)
			canvas.drawText(valueString, 0f, yAxisValue - LEFT_LABEL_BOTTOM_MARGIN, textPaint)
		}
	}

	internal fun getHeightWithMargins() =
		height.toFloat() - bottomTextHeight - bottomTextDescent - BOTTOM_LABELS_TOP_MARGIN

	private fun drawBottomLabels(canvas: Canvas) {
		var lastX = 0f
		val y = (height - bottomTextDescent).toFloat()

		for (index in 0 until bottomLabelsList.size) {
			val x = (index - startIndex) * stepX
			if (lastX != 0f && x - lastX < 10.dpToPxFloat()) continue
			val date = bottomLabelsList[index]
			canvas.drawText(date.shortDate, x, y, textPaint)
			lastX = x + longestWidth
		}
	}

	/**
	 * Changing block
	 */

	override fun toggleLine(lineIndex: Int) {
		val oldMaxValue = getVerticalMaxValue()

		val line = chartLines[lineIndex]
		line.isEnabled = !line.isEnabled
		resetVerticalMaxNum()
		updateLeftLabelsList()

		val newMaxValue = getVerticalMaxValue()
		callAnimation(oldMaxValue, newMaxValue)
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		val widthFloat = width.toFloat()
		stepX = (widthFloat / bottomLabelsList.size.toFloat()) * 4
		xValuesToDisplay = (widthFloat / stepX).toInt() + 1
		startIndex = bottomLabelsList.size - xValuesToDisplay

		if (needUpdatePreview) {
			onDataLoaded()
			needUpdatePreview = false
		}
	}

	fun update(newStart: Float, newLength: Float) {
		popupHandler.reset()
		val widthFloat = width.toFloat()
		val ratio = widthFloat / newLength
		stepX = (widthFloat / bottomLabelsList.size.toFloat()) * ratio
		xValuesToDisplay = (widthFloat / stepX).toInt() + 1
		startIndex = ((newStart / stepX) * ratio).toInt()
		postInvalidate()
	}

	private fun callAnimation(oldMaxValue: Int, newMaxValue: Int) {
		valueAnimator.setValues(PropertyValuesHolder.ofInt(LINES_PROPERTY, oldMaxValue, newMaxValue))
		valueAnimator.start()
	}

	override fun updateTheme(lightThemeEnabled: Boolean) {
		isLightThemeEnabled = lightThemeEnabled

		val shadowProperty = prepareProperty(POPUP_SHADOW_PROPERTY, R.color.grey_50, R.color.black_50)
		val popupProperty = prepareProperty(POPUP_PROPERTY, R.color.white, R.color.darkBackground)
		val smallDotProperty = prepareProperty(SMALL_DOT_PROPERTY, R.color.white, R.color.darkBackground)
		val textProperty = prepareProperty(TEXT_PROPERTY, R.color.labels_text_light, R.color.labels_text_dark)
		val backgroundLineProperty = prepareProperty(BACKGROUND_LINE_PROPERTY, R.color.lines_light, R.color.lines_dark)

		colorAnimation.setValues(shadowProperty, popupProperty, smallDotProperty, textProperty, backgroundLineProperty)
		colorAnimation.start()
	}

	private fun prepareProperty(propertyName: String, @ColorRes lightColor: Int, @ColorRes darkColor: Int
	): PropertyValuesHolder {
		val colorLight = ContextCompat.getColor(context, lightColor)
		val colorDark = ContextCompat.getColor(context, darkColor)

		val colorFrom = if (isLightThemeEnabled) colorDark else colorLight
		val colorTo = if (isLightThemeEnabled) colorLight else colorDark

		return PropertyValuesHolder.ofObject(propertyName, ArgbEvaluator(), colorFrom, colorTo)
	}

	/**
	 * On touch block
	 */

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent): Boolean {
		when (event.action) {
			MotionEvent.ACTION_DOWN -> showPopupAt(event.x)
			MotionEvent.ACTION_MOVE -> showPopupAt(event.x)
			else -> return false
		}
		return true
	}

	private fun showPopupAt(x: Float) {
		if (chartLines.isEmpty()) return
		popupHandler.reset()

		var resultX = x

		if (resultX < MARGIN_8) resultX = MARGIN_8
		else if (resultX > width.toFloat() - MARGIN_8) resultX = width.toFloat() - MARGIN_8

		val position = startIndex + (resultX / stepX).toInt()
		if (position < 0 || position >= bottomLabelsList.size) return

		for (line in chartLines) {
			if (!line.isEnabled) continue
			val dotValue = line.dataValues[position]
			resultX = (position - startIndex) * stepX
			popupHandler.addPopup(Popup(bottomLabelsList[position], line, dotValue, resultX, line.color))
			postInvalidate()
		}
	}
}