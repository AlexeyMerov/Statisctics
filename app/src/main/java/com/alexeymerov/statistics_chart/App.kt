package com.alexeymerov.statistics_chart

import android.app.Application
import com.alexeymerov.statistics_chart.utils.SPHelper

class App : Application() {

	companion object {
		const val THEME_SHARED_KEY = "isLightThemeEnabled"
	}

	override fun onCreate() {
		super.onCreate()
		SPHelper.init(this, "statistics")
	}
}