package io.latentai.android.lre.sample.processor

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint


/**
 * Utility class for drawing labels on a canvas.
 */
class LabelOverlay(size: Float) {
    // paints for text and background
    private val foreground = Paint()
    private val background: Paint

    /**
     * Create a custom coloured overlay for the given text size.
     *
     * @param size
     * the size of the text in the overlay.
     */
    init {
        foreground.textSize = size
        foreground.style = Paint.Style.FILL
        foreground.isAntiAlias = false
        foreground.alpha = 255
        this.background = Paint()
        background.textSize = size
        background.style = Paint.Style.FILL
        background.strokeWidth = size / 8
        background.isAntiAlias = false
        background.alpha = 160
    }

    /**
     * Bind a textual value to a canvas at the given location.
     *
     * @param canvas
     * the canvas to add the label overlay to.
     * @param background
     * the background colour to bind to.
     * @param x
     * the horizontal position of the label.
     * @param y
     * the vertical position of the label.
     * @param text
     * the input text to overlay on the image.
     */
    fun bind(canvas: Canvas, background: Int, x: Float, y: Float, text: String?) {
        bind(canvas, Color.WHITE, background, x, y, text)
    }

    /**
     * Bind a textual value to a canvas at the given location.
     *
     * @param canvas
     * the canvas to add the label overlay to.
     * @param foreground
     * the foreground colour to bind to.
     * @param background
     * the background colour to bind to.
     * @param x
     * the horizontal position of the label.
     * @param y
     * the vertical position of the label.
     * @param text
     * the input text to overlay on the image.
     */
    fun bind(canvas: Canvas, foreground: Int, background: Int, x: Float, y: Float, text: String?) {
        val width = this.background.measureText(text)
        val length = this.background.textSize
        val textual = Paint(this.foreground)
        val highlight = Paint(this.background)
        textual.color = foreground
        highlight.color = background
        canvas.drawRect(x, (y + length.toInt()), (x + width.toInt()), y, highlight)
        canvas.drawText(text!!, x, (y + length), textual)
    }
}