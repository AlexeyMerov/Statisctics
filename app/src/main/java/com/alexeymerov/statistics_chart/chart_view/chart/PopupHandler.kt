package com.alexeymerov.statistics_chart.chart_view.chart

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.alexeymerov.statistics_chart.App.Companion.MARGIN_12
import com.alexeymerov.statistics_chart.App.Companion.MARGIN_16
import com.alexeymerov.statistics_chart.App.Companion.MARGIN_2
import com.alexeymerov.statistics_chart.App.Companion.MARGIN_6
import com.alexeymerov.statistics_chart.App.Companion.MARGIN_8
import com.alexeymerov.statistics_chart.R
import com.alexeymerov.statistics_chart.model.Popup
import com.alexeymerov.statistics_chart.utils.dpToPxFloat
import com.alexeymerov.statistics_chart.utils.formatK
import com.alexeymerov.statistics_chart.utils.formatM
import com.alexeymerov.statistics_chart.utils.spToPxFloat

class PopupHandler(private val lineView: LineView) {

	private val DOT_SMALL_RADIUS = 3.dpToPxFloat()
	private val DOT_BIG_RADIUS = MARGIN_6
	private val POPUP_RADIUS = MARGIN_6

	private var initialX = 0f
	private var startX = 0f
	private var endX = 0f
	private val startY = MARGIN_8
	private var endY = 0f

	private val rectText = Rect()

	private val popupTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		textSize = 12.spToPxFloat()
		textAlign = Paint.Align.LEFT
		typeface = Typeface.DEFAULT_BOLD
	}

	private val backgroundLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		lineView.updatePaint(lineView.context, this, R.color.lines_light, R.color.lines_dark)
		style = Paint.Style.STROKE
		strokeWidth = 1f.dpToPxFloat()
	}

	private val bigDotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

	private val smallDotPaint = Paint(bigDotPaint).apply {
		lineView.updatePaint(lineView.context, this, R.color.white, R.color.darkBackground)
	}

	private val popupRectPaint = Paint().apply {
		lineView.updatePaint(lineView.context, this, R.color.white, R.color.darkBackground)
		updateShadow(this)
	}

	private val popupList = mutableListOf<Popup>()

	fun drawPopup(canvas: Canvas) {
		if (popupList.isEmpty()) return

		initialX = popupList[0].x
		val heightWithMargins = lineView.getHeightWithMargins()
		val yStep = heightWithMargins / lineView.getVerticalMaxValue().toFloat()
		val dateString = popupList[0].date.fullDate
		var longestWord = dateString

		canvas.drawLine(initialX, 0f, initialX, heightWithMargins, backgroundLinePaint)
		for (popup in popupList) {
			if (!popup.line.isEnabled) continue

			val value = popup.value
			val valueString = value.toString()
			if (popup.line.name.length > longestWord.length) longestWord = popup.line.name
			if (valueString.length > longestWord.length) longestWord = valueString

			val y = heightWithMargins - (value.toFloat() * yStep)
			bigDotPaint.color = popup.color
			canvas.drawCircle(initialX, y, DOT_BIG_RADIUS, bigDotPaint)
			canvas.drawCircle(initialX, y, DOT_SMALL_RADIUS, smallDotPaint)
		}

		popupTextPaint.textSize = 12.spToPxFloat()
		popupTextPaint.typeface = Typeface.DEFAULT_BOLD
		popupTextPaint.getTextBounds(longestWord, 0, longestWord.length, rectText)
		val lineHeight = rectText.height().toFloat()


		calculateXY(lineHeight)
		canvas.drawRoundRect(RectF(startX, startY, endX, endY), POPUP_RADIUS, POPUP_RADIUS, popupRectPaint)
		drawPopupText(canvas, dateString, lineHeight)
	}

	private fun calculateXY(lineHeight: Float) {
		startX = initialX - MARGIN_16
		endX = startX + rectText.width() + MARGIN_16

		if (endX + MARGIN_16 >= lineView.width) {
			startX = initialX + MARGIN_16 - rectText.width() - MARGIN_16
			endX = startX + rectText.width() + MARGIN_16
		}

		if (startX < 0) startX = MARGIN_8
		if (endX > lineView.width) {
			val popupWidth = endX - startX
			endX = lineView.width.toFloat() - MARGIN_8
			startX = endX - popupWidth
		}

		val calculatedTextXY = calculatePopupXY(lineHeight)
		if (calculatedTextXY.first + MARGIN_8 > endX) endX = calculatedTextXY.first + MARGIN_8
		endY = calculatedTextXY.second + MARGIN_8
	}

	private fun drawPopupText(canvas: Canvas, dateString: String, lineHeight: Float) {
		val startXFirstColumn = startX + MARGIN_8
		var startXSecondColumn = startXFirstColumn
		var lastTextY = startY + MARGIN_16

		popupTextPaint.color = if (lineView.isLightThemeEnabled) Color.BLACK else Color.WHITE
		popupTextPaint.textSize = 12.spToPxFloat()
		canvas.drawText(dateString, startXFirstColumn, lastTextY, popupTextPaint)

		lastTextY += lineHeight + MARGIN_12
		for ((index, popup) in popupList.withIndex()) {
			if (!popup.line.isEnabled) continue
			val valueString = getValueString(popup)
			calculateStringBounds(valueString)

			if (index == 0) startXSecondColumn = startXFirstColumn + rectText.width() + MARGIN_16
			val startXForText: Float
			when {
				index % 2 == 0 -> {
					startXForText = startXFirstColumn
					if (index > 0) lastTextY += (lineHeight * 2) + MARGIN_12
				}
				else -> startXForText = startXSecondColumn
			}

			popupTextPaint.color = popup.color
			popupTextPaint.typeface = Typeface.DEFAULT_BOLD
			popupTextPaint.textSize = 14.spToPxFloat()
			canvas.drawText(valueString, startXForText, lastTextY, popupTextPaint)

			popupTextPaint.textSize = 10.spToPxFloat()
			popupTextPaint.typeface = Typeface.DEFAULT
			canvas.drawText(popup.line.name, startXForText, lastTextY + lineHeight + MARGIN_2, popupTextPaint)
		}
	}

	private fun calculatePopupXY(lineHeight: Float): Pair<Float, Float> {
		val startXFirstColumn = startX + MARGIN_8
		var startXSecondColumn = startXFirstColumn
		var lastTextY = startY + MARGIN_16 + lineHeight + MARGIN_12

		for ((index, popup) in popupList.withIndex()) {
			if (!popup.line.isEnabled) continue
			val valueString = getValueString(popup)
			calculateStringBounds(valueString)

			if (index == 0) startXSecondColumn = startXFirstColumn + rectText.width() + MARGIN_16
			if (index % 2 == 0 && index > 0) lastTextY += (lineHeight * 2) + MARGIN_12
		}
		val endX = startXSecondColumn + rectText.width()
		val endY = lastTextY + lineHeight + MARGIN_2
		return endX to endY
	}

	private fun calculateStringBounds(valueString: String) {
		popupTextPaint.textSize = 14.spToPxFloat()
		popupTextPaint.getTextBounds(valueString, 0, valueString.length, rectText)
	}

	private fun getValueString(popup: Popup): String {
		val value = popup.value
		return when {
			value > 1000000 -> value.formatM()
			value > 1000 -> value.formatK()
			else -> value.toString()
		}
	}

	fun reset() = popupList.clear()

	fun addPopup(popup: Popup) {
		popupList.add(popup)
	}

	fun updateRectColor(color: Int) {
		popupRectPaint.color = color
	}

	fun updateSmallDotColor(color: Int) {
		smallDotPaint.color = color
	}

	fun updateShadowColor(color: Int) = updateShadow(popupRectPaint, color)

	private fun updateShadow(paint: Paint, color: Int? = null) {
		val shadowColor = when (color) {
			null -> {
				val colorRes = if (lineView.isLightThemeEnabled) R.color.grey_50 else R.color.black_50
				ContextCompat.getColor(lineView.context, colorRes)
			}
			else -> color
		}
		paint.setShadowLayer(2.dpToPxFloat(), 0f, 0f, shadowColor)
	}
}