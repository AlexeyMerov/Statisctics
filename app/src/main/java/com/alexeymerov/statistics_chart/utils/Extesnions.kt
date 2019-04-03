package com.alexeymerov.statistics_chart.utils

import android.content.res.Resources
import java.text.DecimalFormat

fun Int.dpToPx() = (this * Resources.getSystem().displayMetrics.density).toInt()
fun Int.dpToPxFloat() = this * Resources.getSystem().displayMetrics.density
fun Float.dpToPxFloat() = this * Resources.getSystem().displayMetrics.density

fun Int.spToPxFloat() = this * Resources.getSystem().displayMetrics.scaledDensity

private val decimalFormat = DecimalFormat().apply {
	isDecimalSeparatorAlwaysShown = false
	applyPattern("#.#")
}

fun Int.formatM() = String.format("%sm", decimalFormat.format(this / 1000000.0))
fun Int.formatK() = String.format("%sk", decimalFormat.format(this / 1000.0))


