package com.fennecstero

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GridOverlayView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint()

    init {
        paint.color = 0x80FFFFFF.toInt() // Semi-transparent white
        paint.strokeWidth = 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Draw vertical lines
        for (i in 1..2) {
            canvas.drawLine(i * width / 3, 0f, i * width / 3, height, paint)
        }

        // Draw horizontal lines
        for (i in 1..2) {
            canvas.drawLine(0f, i * height / 3, width, i * height / 3, paint)
        }
    }
}
