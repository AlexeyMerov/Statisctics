package com.alexeymerov.statistics_chart.interfaces

import android.content.Context
import android.graphics.Paint
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

interface UpdatableTheme {

	var isLightThemeEnabled: Boolean

	fun updateTheme(lightThemeEnabled: Boolean)

	fun updatePaint(context: Context, paint: Paint, @ColorRes lightColor: Int, @ColorRes darkColor: Int) {
		val colorRes = if (isLightThemeEnabled) lightColor else darkColor
		paint.color = ContextCompat.getColor(context, colorRes)
	}
}

interface PreviewScrollListener {
	fun moveOrResize(newXPosition: Float, newLength: Float)
}