package com.alexeymerov.statistics_chart.chart_view.preview_view

import android.animation.ArgbEvaluator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF

import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.alexeymerov.statistics_chart.App
import com.alexeymerov.statistics_chart.R
import com.alexeymerov.statistics_chart.interfaces.PreviewScrollListener
import com.alexeymerov.statistics_chart.interfaces.UpdatableTheme
import com.alexeymerov.statistics_chart.utils.SPHelper
import com.alexeymerov.statistics_chart.utils.dpToPxFloat

class SelectorView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), UpdatableTheme {

	override var isLightThemeEnabled = SPHelper.getShared(App.THEME_SHARED_KEY, true)

	private val IN_ACTIVE_PROPERTY = "inActivePaint"
	private val BOUNDS_PROPERTY = "boundsPaint"

	private val touchEventProcessor = TouchEventProcessor()

	private val leftRect = RectF()
	private val movableRect = RectF()
	private val rightRect = RectF()

	private val movableRectTop = RectF()
	private val movableRectBottom = RectF()
	private val movableRectLeft = RectF()
	private val movableRectRight = RectF()

	private val verticalPadding = 2.dpToPxFloat()
	private val horizontalPadding = 5.dpToPxFloat()

	private val defaultPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.FILL
		xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
	}
	private val activePaint = Paint(defaultPaint).apply { color = Color.TRANSPARENT }
	private val inActivePaint = Paint(defaultPaint).apply {
		updatePaint(context, this, R.color.preview_inactive_light, R.color.preview_inactive_dark)
	}
	private val boundsPaint = Paint(defaultPaint).apply {
		updatePaint(context, this, R.color.preview_bounds_light, R.color.preview_bounds_dark)
	}

	private var mViewHeight = 0f
	private var mViewWidth = 0f

	private var startX = 0f
	private var endX = 0f
	private var minSize = 25.dpToPxFloat()
	private var previewWidth = 25.dpToPxFloat()

	private var scrollListener: PreviewScrollListener? = null

	private var isInMovableMode = false
	private var leftExpansionMode = false
	private var rightExpansionMode = false

	private var needCallListener = false

	private val colorAnimation = ValueAnimator().apply {
		duration = 250
		addUpdateListener { animator ->
			inActivePaint.color = animator.getAnimatedValue(IN_ACTIVE_PROPERTY) as Int
			boundsPaint.color = animator.getAnimatedValue(BOUNDS_PROPERTY) as Int
			postInvalidate()
		}
	}

	init {
		setLayerType(View.LAYER_TYPE_SOFTWARE, null)
	}

	override fun updateTheme(lightThemeEnabled: Boolean) {
		isLightThemeEnabled = lightThemeEnabled

		val inActiveProperty =
			prepareProperty(IN_ACTIVE_PROPERTY, R.color.preview_inactive_light, R.color.preview_inactive_dark)
		val boundsProperty = prepareProperty(BOUNDS_PROPERTY, R.color.preview_bounds_light, R.color.preview_bounds_dark)

		colorAnimation.setValues(inActiveProperty, boundsProperty)
		colorAnimation.start()
	}

	private fun prepareProperty(propertyName: String, @ColorRes lightColor: Int, @ColorRes darkColor: Int
	): PropertyValuesHolder {
		val colorLight = ContextCompat.getColor(context, lightColor)
		val colorDark = ContextCompat.getColor(context, darkColor)

		val colorFrom = if (isLightThemeEnabled) colorDark else colorLight
		val colorTo = if (isLightThemeEnabled) colorLight else colorDark

		return PropertyValuesHolder.ofObject(propertyName, ArgbEvaluator(), colorFrom, colorTo)
	}

	fun setScrollListener(listener: PreviewScrollListener) {
		scrollListener = listener
	}

	fun setPreviewSize(newSize: Float) {
		previewWidth = newSize
		startX = mViewWidth - newSize
		endX = mViewWidth
		updatePositions()
		invalidate()
		needCallListener = true
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val viewWidth = MeasureSpec.getSize(widthMeasureSpec)
		val viewHeight = MeasureSpec.getSize(heightMeasureSpec)

		mViewWidth = viewWidth.toFloat()
		mViewHeight = viewHeight.toFloat()

		updatePositions()
		setMeasuredDimension(viewWidth, viewHeight)
	}

	private fun updatePositions() {
		movableRect.set(startX, verticalPadding, endX, mViewHeight - verticalPadding)

		movableRectLeft.set(startX, 0f, startX + horizontalPadding, mViewHeight)
		movableRectRight.set(endX - horizontalPadding, 0f, endX, mViewHeight)

		movableRectTop.set(startX, 0f, endX, movableRect.top)
		movableRectBottom.set(startX, movableRect.bottom, endX, mViewHeight)

		leftRect.set(0f, 0f, startX, mViewHeight)
		rightRect.set(endX, 0f, mViewWidth, mViewHeight)

		if (needCallListener) scrollListener?.moveOrResize(startX, previewWidth)
	}

	override fun onDraw(canvas: Canvas) {
		canvas.drawRect(movableRect, activePaint)
		canvas.drawRect(movableRectLeft, boundsPaint)
		canvas.drawRect(movableRectRight, boundsPaint)

		canvas.drawRect(movableRectTop, boundsPaint)
		canvas.drawRect(movableRectBottom, boundsPaint)

		canvas.drawRect(leftRect, inActivePaint)
		canvas.drawRect(rightRect, inActivePaint)
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent) = touchEventProcessor.onTouchEvent(event)

	private inner class TouchEventProcessor {
		private var dX = 0f
		private var oldX = 0f
		private var newX = 0f

		private var leftBound = 0f
		private var rightBound = 0f

		private var leftSliderLeft = 0f
		private var leftSliderRight = 0f

		private var rightSliderLeft = 0f
		private var rightSliderRight = 0f

		fun onTouchEvent(event: MotionEvent): Boolean {
			oldX = event.x
			newX = event.rawX
			if (newX > width) newX = width.toFloat()

			leftBound = movableRect.left
			rightBound = movableRect.right

			leftSliderLeft = movableRectLeft.left
			leftSliderRight = movableRectLeft.right

			rightSliderLeft = movableRectRight.left
			rightSliderRight = movableRectRight.right

			when (event.action) {
				MotionEvent.ACTION_DOWN -> handleActionDown()
				MotionEvent.ACTION_MOVE -> handleActionMove()
				MotionEvent.ACTION_UP -> {
					leftExpansionMode = false
					rightExpansionMode = false
					isInMovableMode = false
				}
				else -> return false
			}
			return true
		}

		private fun handleActionDown() {
			dX = startX - newX
			when {
				onRightSliderPressed() -> rightExpansionMode = true
				onLeftSliderPressed() -> leftExpansionMode = true
			}
		}

		private fun handleActionMove() {
			when {
				(isInMovableMode || onPreviewTouched()) && !leftExpansionMode && !rightExpansionMode -> movePreview()
				leftExpansionMode || rightExpansionMode -> resizePreview()
			}
			updatePositions()
			invalidate()
		}

		private fun onPreviewTouched() = oldX in leftSliderRight..rightSliderLeft

		private fun movePreview() {
			isInMovableMode = true
			var newX = newX + dX
			newX = when {
				newX < 0f -> 0f
				newX + previewWidth > width -> (width - previewWidth)
				else -> newX
			}
			startX = newX
			endX = newX + previewWidth
		}

		private fun resizePreview() {
			when {
				leftExpansionMode && newX < endX - minSize -> handleLeftExtension()
				rightExpansionMode && newX > startX + minSize -> handleRightExpansion()
			}
		}

		private fun onLeftSliderPressed() = oldX in leftSliderLeft..leftSliderRight

		private fun onRightSliderPressed() = oldX in rightSliderLeft..rightSliderRight

		private fun handleLeftExtension() {
			previewWidth = endX - newX
			startX = newX
		}

		private fun handleRightExpansion() {
			previewWidth = newX - startX
			endX = newX
		}
	}
}