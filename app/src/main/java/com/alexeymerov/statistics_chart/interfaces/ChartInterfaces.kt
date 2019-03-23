package com.alexeymerov.statistics_chart.interfaces

interface UpdatableTheme {
	fun updateTheme(lightThemeEnabled: Boolean)
}

interface PreviewScrollListener {
	fun moveOrResize(newXPosition: Float, newLength: Float)
}