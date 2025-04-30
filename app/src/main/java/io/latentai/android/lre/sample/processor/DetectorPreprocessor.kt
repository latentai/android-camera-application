package io.latentai.android.lre.sample.processor

import android.graphics.Bitmap
import android.graphics.Canvas
import io.latentai.android.lre.inference.DataType
import io.latentai.android.lre.model.LREMetaData
import io.latentai.android.lre.model.Model
import io.latentai.android.lre.processor.InferPreprocessor
import io.latentai.android.lre.sample.processor.Transformers.create

/**
 * This is developer implementation to preprocess input data
 * For this example when input data is:
 * IMG_BITMAP -- properly converting to ARGB_888 and resize to model expected size
 * IMG_BYTES -- read jpg to bytes, no need to do anything, straight pass through
 */
class DetectorPreprocessor(model: Model?) : InferPreprocessor(model) {
    override fun <T> preprocess(data: T, dataType: DataType): T {
        /**
         * This preprocessor to support two types of data:
         * Image as Bitmap
         * Image as bytes array
         */

        // Preprocess the bitmap image to desired size and config
        if (dataType == DataType.IMG_BITMAP) {
            val bitmap = data as Bitmap

            // create a new bitmap to store crop
            val bitmapForInference = Bitmap.createBitmap(
                LREMetaData.getInputWidth(),
                LREMetaData.getInputHeight(),
                Bitmap.Config.ARGB_8888
            )
            // create a matrix to apply cropping
            val bitmapCropTransformer = create(
                bitmap.width,
                bitmap.height,
                LREMetaData.getInputWidth(),
                LREMetaData.getInputHeight(),
                true
            )

            // convert the bitmap to the model size via crop
            Canvas(bitmapForInference)
                .drawBitmap(bitmap, bitmapCropTransformer, null)

            return bitmapForInference as T
        } else if (dataType == DataType.IMG_BYTES) {
            // already bytes data
            return data as T
        } else {
            return null as T
        }
    }
}
