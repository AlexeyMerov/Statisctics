package com.alexeymerov.statistics_chart

import android.app.Application
import com.alexeymerov.statistics_chart.utils.SPHelper
import com.alexeymerov.statistics_chart.utils.dpToPxFloat

class App : Application() {

	companion object {
		const val THEME_SHARED_KEY = "isLightThemeEnabled"

		val MARGIN_2 = 2.dpToPxFloat()
		val MARGIN_4 = 4.dpToPxFloat()
		val MARGIN_6 = 6.dpToPxFloat()
		val MARGIN_12 = 12.dpToPxFloat()

		val MARGIN_8 = 8.dpToPxFloat()
		val MARGIN_16 = MARGIN_8 * 2f
	}

	override fun onCreate() {
		super.onCreate()
		SPHelper.init(this, "statistics")
	}
}