package com.meenbeese.chronos.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

import androidx.core.content.ContextCompat

import com.meenbeese.chronos.R
import com.meenbeese.chronos.utils.DimenUtils

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin


/**
 * Display a progress circle with text in
 * the center.
 */
class ProgressTextView : View {

    private var progress: Long = 0
    private var maxProgress: Long = 0
    private var referenceProgress: Long = 0
    private var text: String? = null
    private var padding: Int = DimenUtils.dpToPx(4f)

    private val lineColor = ContextCompat.getColor(context, R.color.colorAccent)
    private val circleColor = ContextCompat.getColor(context, R.color.textColorPrimary)
    private val referenceCircleColor = ContextCompat.getColor(context, R.color.colorAccent)
    private val backgroundColor = ContextCompat.getColor(context, R.color.colorIndeterminateText)
    private val textColorPrimary = ContextCompat.getColor(context, R.color.colorIndeterminateText)

    private var linePaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = padding.toFloat()
        color = lineColor
    }

    private var circlePaint: Paint = Paint().apply {
        isAntiAlias = true
        color = circleColor
    }

    private var referenceCirclePaint: Paint = Paint().apply {
        isAntiAlias = true
        color = referenceCircleColor
    }

    private var backgroundPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = padding.toFloat()
        color = backgroundColor
    }

    private var textPaint: Paint = Paint().apply {
        color = textColorPrimary
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = DimenUtils.spToPx(34f).toFloat()
        isFakeBoldText = true
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /**
     * Set the text (time) to display in the center
     * of the view.
     */
    fun setText(text: String) {
        this.text = text
        invalidate()
    }

    /**
     * Set the current progress value.
     */
    @JvmOverloads
    fun setProgress(progress: Long, animate: Boolean = false) {
        if (animate) {
            ValueAnimator.ofFloat(this.progress.toFloat(), progress.toFloat()).apply {
                interpolator = LinearInterpolator()
                addUpdateListener { valueAnimator ->
                    (valueAnimator.animatedValue as? Float)?.toLong()?.let { value ->
                        setProgress(value, false)
                    }
                }
                start()
            }
        } else {
            this.progress = progress
            postInvalidate()
        }
    }

    /**
     * Set the largest progress that has been acquired so far.
     */
    @JvmOverloads
    fun setMaxProgress(maxProgress: Long, animate: Boolean = false) {
        if (animate) {
            ValueAnimator.ofFloat(this.maxProgress.toFloat(), maxProgress.toFloat()).apply {
                interpolator = LinearInterpolator()
                addUpdateListener { valueAnimator ->
                    (valueAnimator.animatedValue as? Float)?.toLong()?.let { value ->
                        setMaxProgress(value, false)
                    }
                }
                start()
            }
        } else {
            this.maxProgress = maxProgress
            postInvalidate()
        }
    }

    /**
     * Set the progress value of the reference dot (?) on
     * the circle. Mostly used in the stopwatch, to indicate
     * the previous/best lap time.
     */
    fun setReferenceProgress(referenceProgress: Long) {
        this.referenceProgress = referenceProgress
        postInvalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val size = measuredWidth
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val size = min(width, height)
        val sidePadding = padding * 3
        canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 2 - sidePadding).toFloat(), if (maxProgress in 1 until progress) linePaint else backgroundPaint)

        if (maxProgress > 0) {
            val angle = 360f * progress / maxProgress
            val referenceAngle = 360f * referenceProgress / maxProgress

            val path = Path()
            path.arcTo(RectF(sidePadding.toFloat(), sidePadding.toFloat(), (size - sidePadding).toFloat(), (size - sidePadding).toFloat()), -90f, angle, true)
            canvas.drawPath(path, linePaint)

            canvas.drawCircle(size / 2 + cos((angle - 90) * Math.PI / 180).toFloat() * (size / 2 - sidePadding), size / 2 + sin((angle - 90) * Math.PI / 180).toFloat() * (size / 2 - sidePadding), (2 * padding).toFloat(), circlePaint)
            if (referenceProgress != 0L)
                canvas.drawCircle(size / 2 + cos((referenceAngle - 90) * Math.PI / 180).toFloat() * (size / 2 - sidePadding), size / 2 + sin((referenceAngle - 90) * Math.PI / 180).toFloat() * (size / 2 - sidePadding), (2 * padding).toFloat(), referenceCirclePaint)
        }

        text?.let { str ->
            canvas.drawText(str, (size / 2).toFloat(), size / 2 - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
        }
    }
}
