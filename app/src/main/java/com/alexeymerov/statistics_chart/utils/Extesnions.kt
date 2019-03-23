package com.alexeymerov.statistics_chart.utils

import android.content.res.Resources

fun Int.dpToPx() = (this * Resources.getSystem().displayMetrics.density).toInt()
fun Int.dpToPxFloat() = (this * Resources.getSystem().displayMetrics.density)
fun Float.dpToPxFloat() = (this * Resources.getSystem().displayMetrics.density)

