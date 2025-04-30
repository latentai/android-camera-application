package io.latentai.android.lre.sample.processor

import android.graphics.Bitmap
import android.graphics.Matrix
import kotlin.math.max

object Transformers {
    /**
     * Create a matrix for transforming canvas component sizing.
     *
     * @param srcWidth
     * the width of the source component.
     * @param srcHeight
     * the height of the source component.
     * @param dstWidth
     * the width of the target component.
     * @param dstHeight
     * the height of the target component.
     * @param maintainAspectRatio
     * Whether to maintain aspect ratio when resizing.
     * @return
     * a newly constructed [Matrix] for transformation.
     */
    fun create(
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int,
        maintainAspectRatio: Boolean
    ): Matrix {
        // create base matrix
        val matrix = Matrix()

        // Apply scaling if necessary.
        if (srcWidth != dstWidth || srcHeight != dstHeight) {
            val scaleFactorX = dstWidth / srcWidth.toFloat()
            val scaleFactorY = dstHeight / srcHeight.toFloat()

            if (maintainAspectRatio) {
                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                val scaleFactor =
                    max(scaleFactorX.toDouble(), scaleFactorY.toDouble()).toFloat()
                matrix.postScale(scaleFactor, scaleFactor)
            } else {
                // Scale exactly to fill dst from src.
                matrix.postScale(scaleFactorX, scaleFactorY)
            }
        }

        return matrix
    }

    /**
     * Rotate a bitmap by the provided rotation angle.
     *
     * @param source
     * the source bitmap to rotate.
     * @param rotation
     * the angle of rotation to apply.
     * @return
     * a [Bitmap] with rotation applied.
     */
    fun rotate(source: Bitmap, rotation: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        return Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            true
        )
    }
}