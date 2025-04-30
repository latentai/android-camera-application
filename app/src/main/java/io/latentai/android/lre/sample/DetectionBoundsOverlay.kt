package io.latentai.android.lre.sample

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import io.latentai.android.lre.sample.processor.LabelOverlay

class DetectionBoundsOverlay constructor(context: Context?, attributeSet: AttributeSet?) :
    View(context, attributeSet) {

    private val detectionObjects: MutableMap<String, RectF> = mutableMapOf()
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context!!, R.color.light_blue)
        strokeWidth = 8f
    }

    // initialize label drawing utilities
    val metrics = resources.displayMetrics
    val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15f, metrics)
    private var mLabelOverlay: LabelOverlay? = LabelOverlay(size)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        detectionObjects.forEach {
            //draw box bounds
            canvas.drawRect(it.value, paint)
            // draw label
            mLabelOverlay!!.bind(canvas, Color.RED, it.value.left, it.value.top, it.key)
        }
    }

    fun drawLabelAndBounds(detectionBounds: Map<String, RectF>) {
        this.detectionObjects.clear()
        this.detectionObjects.putAll(detectionBounds)
        invalidate()
    }
}