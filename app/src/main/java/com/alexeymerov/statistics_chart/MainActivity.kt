package com.alexeymerov.statistics_chart

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.alexeymerov.statistics_chart.App.Companion.THEME_SHARED_KEY
import com.alexeymerov.statistics_chart.chart_view.ChartView
import com.alexeymerov.statistics_chart.interfaces.UpdatableTheme
import com.alexeymerov.statistics_chart.utils.ChartDataParser
import com.alexeymerov.statistics_chart.utils.SPHelper

class MainActivity : AppCompatActivity(), UpdatableTheme {

	private lateinit var linearLayout: LinearLayout
	private lateinit var scrollView: ScrollView
	private var chartViews = mutableListOf<ChartView>()
	private val chartDataParser by lazy { ChartDataParser() }
	private var themeMenuItem: MenuItem? = null
	private var isLightThemeEnabled = true
	private val moonIcon by lazy { ContextCompat.getDrawable(this, R.drawable.ic_moon) }
	private val sunIcon by lazy { ContextCompat.getDrawable(this, R.drawable.ic_sun) }

	override fun onCreate(savedInstanceState: Bundle?) {
		isLightThemeEnabled = SPHelper.getShared(THEME_SHARED_KEY, true)
		setTheme(if (isLightThemeEnabled) R.style.AppTheme else R.style.AppThemeDark)

		super.onCreate(savedInstanceState)
		setContentView(createLayout())
		chartDataParser.parseJsonRawRes(this, R.raw.chart_data) { allCharts ->
			allCharts.forEach {
				val chartView = ChartView(this).apply {
					layoutParams = layoutParams()
					updateColors(this, R.color.white, R.color.darkBackground, false)
					setTitleText(context.getString(R.string.followers))
				}
				chartView.setDataList(it.first, it.second)
				chartViews.add(chartView)
				linearLayout.addView(chartView)
			}
		}
	}

	private fun createLayout(): View {
		scrollView = object : ScrollView(this) {
			override fun onInterceptTouchEvent(ev: MotionEvent?) = false
		}.apply {
			layoutParams = layoutParams()
			updateColors(this, R.color.grey_50, R.color.darkColorPrimaryDark, false)
		}

		linearLayout = LinearLayout(this).apply {
			layoutParams = layoutParams()
			orientation = LinearLayout.VERTICAL
			updateColors(this, R.color.white, R.color.darkBackground, false)
		}

		scrollView.addView(linearLayout)

		return scrollView
	}

	private fun layoutParams() =
		ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		themeMenuItem = menu?.add(Menu.NONE, 0, Menu.NONE, getString(R.string.switch_themes))
		themeMenuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
		themeMenuItem?.icon = if (isLightThemeEnabled) moonIcon else sunIcon
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem?): Boolean {
		if (item?.itemId == 0) {
			isLightThemeEnabled = !isLightThemeEnabled
			themeMenuItem?.icon = if (isLightThemeEnabled) moonIcon else sunIcon
			SPHelper.setShared(THEME_SHARED_KEY, isLightThemeEnabled)
			updateTheme(isLightThemeEnabled)
		}
		return super.onOptionsItemSelected(item)
	}

	override fun updateTheme(lightThemeEnabled: Boolean) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) updateStatusBar()
		updateToolbar()
		updateColors(scrollView, R.color.grey_50, R.color.darkColorPrimaryDark, true)
		updateColors(linearLayout, R.color.white, R.color.darkBackground, true)
		chartViews.forEach {
			updateColors(it, R.color.white, R.color.darkBackground, true)
			it.updateTheme(isLightThemeEnabled)
			it.invalidate()
		}

		scrollView.invalidate()
		linearLayout.invalidate()
	}

	private fun updateToolbar() {
		val colorLight = ContextCompat.getColor(this, R.color.colorPrimary)
		val colorDark = ContextCompat.getColor(this, R.color.darkColorPrimary)

		val colorFrom = if (isLightThemeEnabled) colorDark else colorLight
		val colorTo = if (isLightThemeEnabled) colorLight else colorDark

		val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
		colorAnimation.duration = 250
		colorAnimation.addUpdateListener { animator ->
			supportActionBar?.setBackgroundDrawable(ColorDrawable((animator.animatedValue as Int)))
		}
		colorAnimation.start()
	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private fun updateStatusBar() {
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
		window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

		val colorLight = ContextCompat.getColor(this, R.color.colorPrimaryDark)
		val colorDark = ContextCompat.getColor(this, R.color.darkColorPrimaryDark)

		val colorFrom = if (isLightThemeEnabled) colorDark else colorLight
		val colorTo = if (isLightThemeEnabled) colorLight else colorDark

		val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
		colorAnimation.duration = 250
		colorAnimation.addUpdateListener { animator -> window.statusBarColor = (animator.animatedValue as Int) }
		colorAnimation.start()
	}

	private fun updateColors(view: View, @ColorRes lightColor: Int, @ColorRes darkColor: Int, needAnimate: Boolean) {
		val colorLight = ContextCompat.getColor(this, lightColor)
		val colorDark = ContextCompat.getColor(this, darkColor)

		val colorFrom = if (isLightThemeEnabled) colorDark else colorLight
		val colorTo = if (isLightThemeEnabled) colorLight else colorDark

		if (needAnimate) {
			val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
			colorAnimation.duration = 250
			colorAnimation.addUpdateListener { animator -> view.setBackgroundColor(animator.animatedValue as Int) }
			colorAnimation.start()
		} else {
			view.setBackgroundColor(colorTo)
		}
	}

}
