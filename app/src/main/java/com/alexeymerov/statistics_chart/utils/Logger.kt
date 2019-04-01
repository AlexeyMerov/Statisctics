package com.alexeymerov.statistics_chart.utils

import android.util.Log
import com.alexeymerov.statistics_chart.BuildConfig

const val TAG = "----- StatisticsApp"

fun Any.toLog(tag: String = TAG) = debugLog(this, tag)

internal inline fun errorLog(any: Any?, tag: String = TAG, crossinline onDone: () -> Unit = {}) {
	whenDebug { Log.e(tag, checkNotNull(any)) }
	onDone.invoke()
}

fun errorLog(exception: Exception, tag: String = TAG) =
	whenDebug { Log.e(tag, checkNotNull(exception.message)) }

fun errorLog(any: Any?, tag: String = TAG, tr: Throwable) =
	whenDebug { Log.e(tag, checkNotNull(any), tr) }

fun debugLog(any: Any?, tag: String = TAG) =
	whenDebug { Log.d(tag, checkNotNull(any)) }

fun debugLog(any: Any?, tag: String = TAG, tr: Throwable) =
	whenDebug { Log.d(tag, checkNotNull(any), tr) }

private inline fun whenDebug(crossinline f: () -> Unit) {
	if (BuildConfig.DEBUG) f.invoke()
}

private fun checkNotNull(any: Any?): String = if (any?.toString() != null) any.toString() else "string for log is null"