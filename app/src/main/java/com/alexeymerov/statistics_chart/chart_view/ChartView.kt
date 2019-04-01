package com.alexeymerov.statistics_chart.chart_view

import android.animation.ArgbEvaluator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.widget.CompoundButtonCompat
import com.alexeymerov.statistics_chart.R
import com.alexeymerov.statistics_chart.THEME_SHARED_KEY
import com.alexeymerov.statistics_chart.chart_view.chart.LineView
import com.alexeymerov.statistics_chart.chart_view.preview_view.PreviewScrollView
import com.alexeymerov.statistics_chart.interfaces.PreviewScrollListener
import com.alexeymerov.statistics_chart.interfaces.UpdatableTheme
import com.alexeymerov.statistics_chart.model.ChartLine
import com.alexeymerov.statistics_chart.utils.SPHelper
import com.alexeymerov.statistics_chart.utils.dpToPx

class ChartView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), PreviewScrollListener, UpdatableTheme {

	private companion object {
		val MARGIN_8 = 8.dpToPx()
		val MARGIN_16 = 16.dpToPx()
		const val COLOR_PROPERTY = "color"
	}

	private val titleView: TextView
	private val lineView: LineView
	private val previewScrollBar: PreviewScrollView
	private val lineNamesList: LinearLayout

	private val totalHeight = 350.dpToPx()
	private val onePartHeight = (totalHeight / 5)
	private val chartHeight = onePartHeight * 4
	private val scrollBarHeight = totalHeight / 6

	private var isLightThemeEnabled = SPHelper.getShared(THEME_SHARED_KEY, true)

	private lateinit var colorAnimation: ValueAnimator

	init {
		setPadding(MARGIN_16, MARGIN_8, MARGIN_16, MARGIN_8)
		orientation = VERTICAL
		clipChildren = true
		clipToPadding = true

		titleView = createTitleView()
		lineView = createLineView()
		previewScrollBar = createPreviewLineView()
		lineNamesList = createLineNamesList()

		addView(titleView)
		addView(lineView)
		addView(previewScrollBar)
		addView(lineNamesList)
	}

	fun setTitleText(text: String? = null) = when (text) {
		null -> {
			titleView.visibility = View.GONE
			titleView.text = ""
		}
		else -> {
			titleView.text = text
			titleView.visibility = View.VISIBLE
		}
	}

	private fun createTitleView() = TextView(context).apply {
		textSize = 18f
		layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
			setMargins(0, 0, 0, MARGIN_8)
		}

		val color = if (isLightThemeEnabled) R.color.light_blue_2 else R.color.light_blue
		setTextColor(ContextCompat.getColor(context, color))
	}

	private fun createLineView() = LineView(context).apply {
		layoutParams = LayoutParams(MATCH_PARENT, chartHeight)
		onDataLoaded = { previewScrollBar.setPreviewSize(lineView.width.toFloat() / 4f) }
	}

	private fun createPreviewLineView() = PreviewScrollView(context).apply {
		isNestedScrollingEnabled = false
		layoutParams = LayoutParams(MATCH_PARENT, scrollBarHeight).apply {
			setMargins(0, MARGIN_16, 0, 0)
		}
		setScrollListener(this@ChartView)
	}

	private fun createLineNamesList() = LinearLayout(context).apply {
		orientation = LinearLayout.VERTICAL
		layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
			setMargins(0, MARGIN_8, 0, 0)
		}
		val color = if (isLightThemeEnabled) R.color.white else R.color.darkBackground
		setBackgroundColor(ContextCompat.getColor(context, color))
	}

	fun setDataList(chartLines: List<ChartLine>, labelsList: List<String>) {
		lineView.setData(chartLines, labelsList)
		previewScrollBar.setData(chartLines, labelsList)

		lineNamesList.removeAllViews()
		chartLines.forEachIndexed { index, chartLine ->
			val checkBox = CheckBox(context).apply {
				text = chartLine.name
				textSize = 16f
				isChecked = true
				val color = if (isLightThemeEnabled) Color.BLACK else Color.WHITE
				setTextColor(color)
				val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
				val colors = intArrayOf(chartLines[index].color, Color.GRAY)
				CompoundButtonCompat.setButtonTintList(this, ColorStateList(states, colors))
				layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
					if (index > 0) setMargins(0, MARGIN_8, 0, 0)
				}

				setOnCheckedChangeListener { _, _ ->
					run {
						lineView.toggleLine(index)
						previewScrollBar.toggleLine(index)
					}
				}
			}
			lineNamesList.addView(checkBox)
		}
	}

	override fun moveOrResize(newXPosition: Float, newLength: Float) {
		lineView.update(newXPosition, newLength)
	}

	override fun updateTheme(lightThemeEnabled: Boolean) {
		isLightThemeEnabled = lightThemeEnabled
		lineView.updateTheme(isLightThemeEnabled)
		previewScrollBar.updateTheme(isLightThemeEnabled)
		val color = if (isLightThemeEnabled) R.color.light_blue_2 else R.color.light_blue
		titleView.setTextColor(ContextCompat.getColor(context, color))
		updateColors(lineNamesList, R.color.white, R.color.darkBackground)
		for (i in 0 until lineNamesList.childCount) {
			val child = lineNamesList.getChildAt(i)
			val textColor = if (isLightThemeEnabled) Color.BLACK else Color.WHITE
			(child as? CheckBox)?.setTextColor(textColor)
		}
	}

	private fun updateColors(view: View, @ColorRes lightColor: Int, @ColorRes darkColor: Int) {
		val colorLight = ContextCompat.getColor(context, lightColor)
		val colorDark = ContextCompat.getColor(context, darkColor)

		val colorFrom = if (isLightThemeEnabled) colorDark else colorLight
		val colorTo = if (isLightThemeEnabled) colorLight else colorDark

		colorAnimation = ValueAnimator().apply { duration = 250 }
		colorAnimation.addUpdateListener { animator -> view.setBackgroundColor(animator.animatedValue as Int) }
		colorAnimation.setValues(PropertyValuesHolder.ofObject(COLOR_PROPERTY, ArgbEvaluator(), colorFrom, colorTo))
		colorAnimation.start()
	}
}