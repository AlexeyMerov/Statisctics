package com.alexeymerov.statistics_chart.chart_view.chart

import android.animation.ArgbEvaluator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.alexeymerov.statistics_chart.R
import com.alexeymerov.statistics_chart.chart_view.AbstractLineView
import com.alexeymerov.statistics_chart.interfaces.UpdatableTheme
import com.alexeymerov.statistics_chart.model.ChartLine
import com.alexeymerov.statistics_chart.model.Popup
import com.alexeymerov.statistics_chart.utils.dpToPx
import com.alexeymerov.statistics_chart.utils.dpToPxFloat
import java.util.Collections

class LineView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AbstractLineView(context, attrs, defStyleAttr), UpdatableTheme {

	private val POPUP_SHADOW_PROPERTY = "popupShadowRectPaint"
	private val POPUP_PROPERTY = "popupRectPaint"
	private val SMALL_DOT_PROPERTY = "smallDotPaint"
	private val TEXT_PROPERTY = "textPaint"
	private val BOTTOM_TEXT_PROPERTY = "bottomTextPaint"
	private val BACKGROUND_LINE_PROPERTY = "backgroundLinePaint"
	private val LINES_PROPERTY = "lines"

	private val BOTTOM_LABELS_TOP_MARGIN = 5.dpToPx()
	private val LEFT_LABEL_BOTTOM_MARGIN = 3.dpToPx()
	private val HORIZONTAL_MARGIN = 3.dpToPxFloat()

	private val DOT_SMALL_RADIUS = 3.dpToPxFloat()
	private val DOT_BIG_RADIUS = 5.dpToPxFloat()

	private val POPUP_RADIUS = 6.dpToPxFloat()
	private val POPUP_SHADOW_RADIUS = 7.dpToPxFloat()
	private val MARGIN_8 = 8.dpToPxFloat()
	private val MARGIN_16 = MARGIN_8 * 2f
	private val MARGIN_32 = MARGIN_16 * 2f
	private val SHADOW_SIZE = 1.5f.dpToPxFloat()

	private var needUpdatePreview = true

	private val leftLabelsList = mutableListOf<String>()
	private val popupList = mutableListOf<Popup>()

	private var leftLabelsModule = 0
	private var bottomTextHeight = 0
	private var bottomTextDescent = 0

	private var leftPadding = 45.dpToPx() / 3 * 2
	private var longestWidth = 0

	private val bottomTextRect = Rect()
	private val linesPath = Path()
	private val rectText = Rect()

	private val popupShadowRectPaint = Paint().apply {
		updatePaint(this, R.color.grey_30, R.color.black_30)
		xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
	}

	private val popupRectPaint = Paint().apply {
		updatePaint(this, R.color.white, R.color.darkBackground)
	}
	private val bigDotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

	private val smallDotPaint = Paint(bigDotPaint).apply {
		updatePaint(this, R.color.white, R.color.darkBackground)
	}

	private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		updatePaint(this, R.color.grey, R.color.mid_blue_2)
		textSize = 12.dpToPxFloat()
		textAlign = Paint.Align.LEFT
		style = Paint.Style.FILL
		typeface = Typeface.DEFAULT_BOLD
	}

	private val bottomTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		updatePaint(this, R.color.grey, R.color.mid_blue_2)
		textSize = 12.dpToPxFloat()
		textAlign = Paint.Align.LEFT
		style = Paint.Style.FILL
		typeface = Typeface.DEFAULT_BOLD
	}

	private val backgroundLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		updatePaint(this, R.color.grey, R.color.black_50)
		style = Paint.Style.STROKE
		strokeWidth = 0.5f.dpToPxFloat()
	}

	private val popupTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		textSize = 16.dpToPxFloat()
		textAlign = Paint.Align.LEFT
		typeface = Typeface.DEFAULT_BOLD
	}

	private val colorAnimation = ValueAnimator().apply {
		duration = 250
		repeatMode = ValueAnimator.RESTART
		addUpdateListener { animator ->
			popupShadowRectPaint.color = animator.getAnimatedValue(POPUP_SHADOW_PROPERTY) as Int
			popupRectPaint.color = animator.getAnimatedValue(POPUP_PROPERTY) as Int
			smallDotPaint.color = animator.getAnimatedValue(SMALL_DOT_PROPERTY) as Int
			textPaint.color = animator.getAnimatedValue(TEXT_PROPERTY) as Int
			bottomTextPaint.color = animator.getAnimatedValue(BOTTOM_TEXT_PROPERTY) as Int
			backgroundLinePaint.color = animator.getAnimatedValue(BACKGROUND_LINE_PROPERTY) as Int
			postInvalidate()
		}
	}

	override var bottomLabelsList = listOf<String>()

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

	override fun setData(newLines: List<ChartLine>, labelsList: List<String>) {
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
			bottomTextPaint.getTextBounds(s, 0, s.length, bottomTextRect)
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
		drawPopup(canvas)
	}

	private fun drawPopup(canvas: Canvas) {
		if (popupList.isEmpty()) return

		val heightWithMargins = getHeightWithMargins()
		val yStep = heightWithMargins / getVerticalMaxValue().toFloat()

		var longestWord = ""

		val x = popupList[0].x
		val dateString = popupList[0].dateString

		canvas.drawLine(x, 0f, x, heightWithMargins, backgroundLinePaint)

		var disabledLinesCount = 0
		for (popup in popupList) {
			if (!popup.line.isEnabled) {
				disabledLinesCount++
				continue
			}

			val value = popup.value
			val fullText = "${popup.line.name}: $value"
			val valueLength = fullText.length
			if (valueLength > longestWord.length) longestWord = fullText

			val y = heightWithMargins - (value.toFloat() * yStep)

			bigDotPaint.color = popup.color
			canvas.drawCircle(x, y, DOT_BIG_RADIUS, bigDotPaint)
			canvas.drawCircle(x, y, DOT_SMALL_RADIUS, smallDotPaint)
		}

		popupTextPaint.getTextBounds(longestWord, 0, longestWord.length - 1, rectText)

		var startX = x + MARGIN_16
		var endX = startX + rectText.width() + MARGIN_32

		if (endX + MARGIN_16 >= width) {
			startX = x - MARGIN_16 - rectText.width() - MARGIN_32
			endX = startX + rectText.width() + MARGIN_32
		}

		val linesCount = popupList.size + 1 - disabledLinesCount
		val startY = MARGIN_8
		val lineHeight = rectText.height().toFloat()
		val endY = startY + (lineHeight * linesCount) + (MARGIN_8 * linesCount) + MARGIN_16

		drawPopupRect(canvas, startX, startY, endX, endY)
		drawPopupText(canvas, startX, startY, dateString, lineHeight)
	}

	private fun drawPopupText(canvas: Canvas, startX: Float, startY: Float,
							  dateString: String, lineHeight: Float) {
		val startXForText = startX + MARGIN_8
		var lastTextY = startY + MARGIN_16
		popupTextPaint.color = if (isLightThemeEnabled) Color.BLACK else Color.WHITE
		canvas.drawText(dateString, startXForText, lastTextY, popupTextPaint)

		lastTextY += MARGIN_8
		for (popup in popupList) {
			if (!popup.line.isEnabled) continue
			lastTextY += lineHeight + MARGIN_8
			popupTextPaint.color = popup.color

			val value = popup.value.toString()
			val text = "${popup.line.name}: $value"
			canvas.drawText(text, startXForText, lastTextY, popupTextPaint)
		}
	}

	private fun drawPopupRect(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float) {
		val mainRect = RectF(startX, startY, endX, endY)
		val shadowRect = RectF(startX - SHADOW_SIZE, startY - SHADOW_SIZE,
				endX + SHADOW_SIZE, endY + SHADOW_SIZE)

		canvas.drawRoundRect(shadowRect, POPUP_SHADOW_RADIUS, POPUP_SHADOW_RADIUS, popupShadowRectPaint)
		canvas.drawRoundRect(mainRect, POPUP_RADIUS, POPUP_RADIUS, popupRectPaint)
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
					0 -> linePath.moveTo(HORIZONTAL_MARGIN, yAxis)
					else -> linePath.lineTo(HORIZONTAL_MARGIN + xIndex * stepX, yAxis)
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
			val text = (currentMaxValue - i).toString()
			linesPath.moveTo(0f, yAxisValue)
			linesPath.lineTo(stopX, yAxisValue)
			canvas.drawText(text, 0f, yAxisValue - LEFT_LABEL_BOTTOM_MARGIN, textPaint)
		}
		canvas.drawPath(linesPath, backgroundLinePaint)
		linesPath.reset()
	}

	private fun getHeightWithMargins() =
		height.toFloat() - bottomTextHeight - bottomTextDescent - BOTTOM_LABELS_TOP_MARGIN

	private fun drawBottomLabels(canvas: Canvas) {
		var lastX = 0f
		val y = (height - bottomTextDescent).toFloat()

		for (index in 0 until bottomLabelsList.size) {
			val x = (index - startIndex) * stepX
			if (lastX != 0f && x - lastX < 10.dpToPxFloat()) continue
			val text = bottomLabelsList[index]
			canvas.drawText(text, x, y, bottomTextPaint)
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
		popupList.clear()
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

		val shadowProperty = prepareProperty(POPUP_SHADOW_PROPERTY, R.color.grey_30, R.color.black_30)
		val popupProperty = prepareProperty(POPUP_PROPERTY, R.color.white, R.color.darkBackground)
		val smallDotProperty = prepareProperty(SMALL_DOT_PROPERTY, R.color.white, R.color.darkBackground)
		val textProperty = prepareProperty(TEXT_PROPERTY, R.color.grey, R.color.mid_blue_2)
		val bottomTextProperty = prepareProperty(BOTTOM_TEXT_PROPERTY, R.color.grey, R.color.mid_blue_2)
		val backgroundLineProperty = prepareProperty(BACKGROUND_LINE_PROPERTY, R.color.grey, R.color.black_50)

		colorAnimation.setValues(shadowProperty, popupProperty, smallDotProperty, textProperty, bottomTextProperty,
				backgroundLineProperty
		)

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

	private fun updatePaint(paint: Paint, @ColorRes lightColor: Int, @ColorRes darkColor: Int) {
		val colorRes = if (isLightThemeEnabled) lightColor else darkColor
		paint.color = ContextCompat.getColor(context, colorRes)
	}

	/**
	 * On touch block
	 */

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent): Boolean {
		when (event.action) {
			MotionEvent.ACTION_DOWN -> showPopupAt(event.rawX)
			MotionEvent.ACTION_MOVE -> showPopupAt(event.rawX)
			else -> return false
		}
		return true
	}

	private fun showPopupAt(x: Float) {
		if (chartLines.isEmpty()) return
		popupList.clear()

		val position = startIndex + (x / stepX).toInt()
		if (position < 0 || position >= bottomLabelsList.size) return

		for (line in chartLines) {
			if (!line.isEnabled) continue
			val dotValue = line.dataValues[position]
			popupList.add(Popup(bottomLabelsList[position], line, dotValue, x, line.color))
			postInvalidate()
		}
	}
}