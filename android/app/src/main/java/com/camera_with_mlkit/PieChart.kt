package com.camera_with_mlkit

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.AttributeSet
import android.widget.LinearLayout

class PieChart(context: Context, attrs: AttributeSet): LinearLayout(context,attrs) {
    var textHeight = 0f
    var textColor = Color.CYAN

    private val textPaint = Paint(ANTI_ALIAS_FLAG).apply{
        color = textColor
        if (textHeight == 0f) {
            textHeight = textSize
        } else {
            textSize = textHeight
        }
    }

    private val piePaint = Paint(ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textSize = textHeight
    }

    private val shadownPaint = Paint(0).apply {
        color = 0x101010
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }

    var showText:Boolean = false
        set(value) {
            field = value
            invalidate()
            requestLayout()
        }

    var textPos:Int = 0

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.PieChart, 0, 0).apply {
            try{
                showText = getBoolean(R.styleable.PieChart_showText, false)
                textPos = getInteger(R.styleable.PieChart_labelPosition, 0)
            }
            finally {
                recycle()
            }
        }
    }

    fun isShowText():Boolean {
        return showText
    }

}