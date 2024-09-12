package com.meenbeese.chronos.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.DecelerateInterpolator

import androidx.core.content.ContextCompat

import com.meenbeese.chronos.R
import com.meenbeese.chronos.utils.DimenUtils


class DaySwitch : View, View.OnClickListener {

    private lateinit var accentPaint: Paint
    private lateinit var textPaint: Paint
    private lateinit var clippedTextPaint: Paint
    private var checked: Float = 0f

    var isChecked: Boolean = false
        set(isChecked) {
            if (isChecked != field) {
                field = isChecked
                animateCheckedState(isChecked)
            }
        }

    private var text: String? = null

    var onCheckedChangeListener: OnCheckedChangeListener? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        setOnClickListener(this)

        accentPaint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.colorAccent)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        textPaint = Paint().apply {
            isAntiAlias = true
            textSize = DimenUtils.dpToPx(18f).toFloat()
            textAlign = Paint.Align.CENTER
        }

        clippedTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = DimenUtils.dpToPx(18f).toFloat()
            textAlign = Paint.Align.CENTER
        }

        updatePaintColors()
    }

    private fun updatePaintColors() {
        textPaint.color = if (isChecked) Color.WHITE else Color.BLACK
        clippedTextPaint.color = if (isChecked) Color.BLACK else Color.WHITE
    }

    private fun animateCheckedState(isChecked: Boolean) {
        val animator = if (isChecked) {
            ValueAnimator.ofFloat(0f, 1f)
        } else {
            ValueAnimator.ofFloat(1f, 0f)
        }

        animator.apply {
            interpolator = if (isChecked) DecelerateInterpolator() else AnticipateOvershootInterpolator()
            addUpdateListener { valueAnimator ->
                checked = valueAnimator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setText(text: String) {
        this.text = text
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val circlePath = Path()
        circlePath.addCircle((width / 2).toFloat(), (height / 2).toFloat(), checked * DimenUtils.dpToPx(18f), Path.Direction.CW)
        circlePath.close()

        canvas.drawPath(circlePath, accentPaint)

        text?.let { str ->
            // Calculate text size to not extend past circle radius ( - 4dp for padding)
            val textWidth = textPaint.measureText(str)
            val circleWidth = DimenUtils.dpToPx(32f)
            if (textWidth > circleWidth) {
                textPaint.textSize *= (circleWidth.toFloat() / textWidth)
                clippedTextPaint.textSize = textPaint.textSize
            }

            // Draw text based on the checked state
            if (checked > 0.5f) {
                canvas.drawText(str, (width / 2).toFloat(), height / 2 - (textPaint.descent() + textPaint.ascent()) / 2, clippedTextPaint)
            } else {
                canvas.drawText(str, (width / 2).toFloat(), height / 2 - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
            }
        }
    }

    override fun onClick(view: View) {
        isChecked = !isChecked
        updatePaintColors()
        onCheckedChangeListener?.onCheckedChanged(this, isChecked)
    }

    interface OnCheckedChangeListener {
        fun onCheckedChanged(daySwitch: DaySwitch, isChecked: Boolean)
    }
}
