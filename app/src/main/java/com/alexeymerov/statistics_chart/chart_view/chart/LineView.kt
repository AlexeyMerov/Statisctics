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
import com.alexeymerov.statistics_chart.model.PopupData
import com.alexeymerov.statistics_chart.utils.SPHelper
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
	private val TEXT_PROPERTY = "labelTextPaint"
	private val BACKGROUND_LINE_PROPERTY = "backgroundLinePaint"

	private val BOTTOM_LABELS_TOP_MARGIN = MARGIN_6
	private val LEFT_LABEL_BOTTOM_MARGIN = MARGIN_4
	private val BOTTOM_LABEL_MIN_GAP = 10.dpToPxFloat()
	internal val TOP_MARGIN = 24.dpToPxFloat()


	private var needUpdatePreview = true

	private val leftLabelsList = mutableListOf<String>()
	private var leftLabelsModule = 0

	private var bottomTextHeight = 0f
	private var bottomTextWidth = 0f

	private val bottomTextRect = Rect()
	private var widthFloat = 0f
	private var heightFloat = 0f

	private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
			labelTextPaint.color = animator.getAnimatedValue(TEXT_PROPERTY) as Int
			backgroundLinePaint.color = animator.getAnimatedValue(BACKGROUND_LINE_PROPERTY) as Int
			postInvalidate()
		}
	}

	var onDataLoaded: () -> Unit = {}

	override fun updateVerticalMaxValue() {
		verticalMaxValue = MIN_VERTICAL_GRID_NUM
		for (chartLine in chartLines) {
			if (!chartLine.isEnabled) continue
			val dataValues = chartLine.dataValues

			var startIndex = if (startIndex < 0) 0 else startIndex

			var toIndex = startIndex + xValuesToDisplay
			if (toIndex >= dataValues.size) toIndex = dataValues.size - 1

			if (startIndex >= toIndex) startIndex = toIndex - xValuesToDisplay

			val subList = dataValues.subList(startIndex, toIndex)
			val maxValue = Collections.max(subList).toFloat()
			if (verticalMaxValue < maxValue) verticalMaxValue = maxValue
		}
	}

	/**
	 * Set new data block
	 */

	override fun setData(newLines: List<ChartLine>, labelsList: List<DateItem>) {
		needUpdatePreview = true
		setBottomTextList(labelsList)
		chartLines = newLines
		updateVerticalMaxValue()
		updateLeftLabelsList()
		postInvalidate()
	}

	private fun updateLeftLabelsList() {
		leftLabelsList.clear()

		val verticalGridNum = getVerticalMaxValue().toInt()

		leftLabelsModule = 1
		if (verticalGridNum >= 10) leftLabelsModule = (verticalGridNum / 10) * 2

		repeat(verticalGridNum) {
			if (leftLabelsModule == 1 || it % leftLabelsModule == 0 || it == 0) {
				val resultValue = it - it % leftLabelsModule
				leftLabelsList.add(resultValue.toString())
			}
		}
	}

	private fun setBottomTextList(labelsList: List<DateItem>) {
		bottomLabelsList = labelsList
		bottomTextWidth = 0f

		val firstItem = bottomLabelsList[0].shortDate
		labelTextPaint.getTextBounds(firstItem, 0, firstItem.length, bottomTextRect)

		val height = bottomTextRect.height().toFloat()
		if (bottomTextHeight < height) bottomTextHeight = height

		for (s in bottomLabelsList) {
			val shortDate = s.shortDate
			labelTextPaint.getTextBounds(shortDate, 0, shortDate.length, bottomTextRect)

			val width = bottomTextRect.width().toFloat()
			if (bottomTextWidth < width) bottomTextWidth = width
		}
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
		val yStep = heightFloat / getVerticalMaxValue()

		for (chartLineEntry in chartLines) {
			if (!chartLineEntry.isEnabled) continue
			val dataValues = chartLineEntry.dataValues
			for (xIndex in 0 until xValuesToDisplay) {
				val dataIndex = xIndex + startIndex
				if (dataIndex >= dataValues.size) continue
				val yAxis = heightFloat - (dataValues[dataIndex].toFloat() * yStep) + TOP_MARGIN
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
		val currentMaxValueInt = currentMaxValue.toInt()
		val yStep = getHeightWithMargins() / currentMaxValue

		for (i in 0 until currentMaxValueInt step leftLabelsModule) {
			val yAxisValue = (i * yStep) + TOP_MARGIN
			val value = currentMaxValueInt - i
			val valueString = when {
				value > 1000000 -> value.formatM()
				value > 1000 -> value.formatK()
				else -> value.toString()
			}

			canvas.drawLine(0f, yAxisValue, widthFloat, yAxisValue, backgroundLinePaint)
			canvas.drawText(valueString, 0f, yAxisValue - LEFT_LABEL_BOTTOM_MARGIN, labelTextPaint)
		}
	}

	internal fun getHeightWithMargins() = heightFloat - bottomTextHeight - BOTTOM_LABELS_TOP_MARGIN

	private fun drawBottomLabels(canvas: Canvas) {
		var lastX = 0f
		val y = heightFloat - bottomTextHeight + TOP_MARGIN

		for (index in 0 until bottomLabelsList.size) {
			val x = (index - startIndex) * stepX
			if (lastX != 0f && x - lastX < BOTTOM_LABEL_MIN_GAP) continue
			val date = bottomLabelsList[index]
			canvas.drawText(date.shortDate, x, y, labelTextPaint)
			lastX = x + bottomTextWidth
		}
	}

	/**
	 * Changing block
	 */

	override fun toggleLine(lineIndex: Int) {
		val oldMaxValue = getVerticalMaxValue()

		val line = chartLines[lineIndex]
		line.isEnabled = !line.isEnabled
		updateVerticalMaxValue()
		updateLeftLabelsList()

		val newMaxValue = getVerticalMaxValue()
		callAnimation(oldMaxValue, newMaxValue)
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		widthFloat = width.toFloat()
		heightFloat = height.toFloat()
		stepX = (widthFloat / bottomLabelsList.size.toFloat()) * 4
		xValuesToDisplay = (widthFloat / stepX).toInt() + 1
		startIndex = bottomLabelsList.size - xValuesToDisplay
		updateVerticalMaxValue()

		if (needUpdatePreview) {
			onDataLoaded()
			needUpdatePreview = false
		}
	}

	fun update(newStart: Float, newLength: Float) {
		popupHandler.reset()
		val oldValue = getVerticalMaxValue()
		stopAnimation()
		val ratio = widthFloat / newLength
		stepX = (widthFloat / bottomLabelsList.size.toFloat()) * ratio
		xValuesToDisplay = (widthFloat / stepX).toInt() + 1
		startIndex = ((newStart / stepX) * ratio).toInt()
		updateVerticalMaxValue()
		val newValue = getVerticalMaxValue()
		callAnimation(oldValue, newValue)
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

		var colorFrom = colorLight
		var colorTo = colorDark

		if (isLightThemeEnabled) {
			colorFrom = colorDark
			colorTo = colorLight
		}

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

		var resultX = when {
			x < MARGIN_8 -> MARGIN_8
			x > widthFloat - MARGIN_8 -> widthFloat - MARGIN_8
			else -> x
		}

		val position = startIndex + (resultX / stepX).toInt()
		if (position < 0 || position >= bottomLabelsList.size) return

		for (line in chartLines) {
			if (!line.isEnabled) continue
			val dotValue = line.dataValues[position]
			resultX = (position - startIndex) * stepX
			popupHandler.addPopup(PopupData(bottomLabelsList[position], line, dotValue, resultX, line.color))
			postInvalidate()
		}
	}
}