package com.alexeymerov.statistics_chart.chart_view.preview_view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import com.alexeymerov.statistics_chart.interfaces.PreviewScrollListener
import com.alexeymerov.statistics_chart.interfaces.UpdatableTheme
import com.alexeymerov.statistics_chart.model.ChartLine

class PreviewScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), UpdatableTheme {

	private var previewLineView: PreviewLineView
	private var selectorView: SelectorView

	init {
		previewLineView = createPreviewLineView()
		addView(previewLineView)

		selectorView = createSelectorView()
		addView(selectorView)
	}

	private fun createPreviewLineView() = PreviewLineView(context).apply {
		layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, MATCH_PARENT)
	}

	private fun createSelectorView() = SelectorView(context).apply {
		layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
		setBackgroundColor(Color.TRANSPARENT)
	}

	fun setScrollListener(listener: PreviewScrollListener) = selectorView.setScrollListener(listener)

	fun setPreviewSize(newSize: Float) = selectorView.setPreviewSize(newSize)

	fun setData(chartLines: List<ChartLine>, labelsList: List<String>) = previewLineView.setData(chartLines, labelsList)

	fun toggleLine(index: Int) = previewLineView.toggleLine(index)

	override fun updateTheme(lightThemeEnabled: Boolean) {
		selectorView.updateTheme(lightThemeEnabled)
	}

}