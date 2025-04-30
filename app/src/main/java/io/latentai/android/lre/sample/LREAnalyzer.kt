package io.latentai.android.lre.sample

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import io.latentai.android.lre.inference.DataType
import io.latentai.android.lre.inference.InferenceEngine
import io.latentai.android.lre.model.LREMetaData
import io.latentai.android.lre.processor.Recognition
import io.latentai.android.lre.result.Results
import io.latentai.android.lre.result.Results.Timings
import java.io.ByteArrayOutputStream



internal class LREAnalyzer(
    private val lreEngine: InferenceEngine?,
    private val callBack: (Results<List<Recognition>, Timings>) -> Unit
) : ImageAnalysis.Analyzer {

    // Results class is from LRE SDK, implemented to carry model recognitions
    // inference timings for this example

    //Note: You can implement your own Results class to meet your model outputs
    var inferenceResults: Results<List<Recognition>, Timings>? = null

    // Rotate the image of the input bitmap
    fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    override fun analyze(image: ImageProxy) {
        // Convert the input image to bitmap and resize to model expected
        // input width and height for model input
        val imgBitmap = image.toBitmap()
        val rawBitmap = imgBitmap.let {
            Bitmap.createScaledBitmap(
                it,
                LREMetaData.getInputWidth(),
                LREMetaData.getInputHeight(),
                false
            )
        }
        val bitmap = rawBitmap.rotate(image.imageInfo.rotationDegrees.toFloat())

        // Invoke LRE SDK API to run inference with input data
        // Receiving model output results, passing to callBack
        if (lreEngine != null) {
            if (lreEngine.getInferenceOptions().dataType == DataType.IMG_BITMAP) {
                (lreEngine.runInference(bitmap) as? Results<List<Recognition>, Timings>).also {
                    inferenceResults = it
                }
            } else if (lreEngine.getInferenceOptions().dataType == DataType.IMG_BYTES) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val bytesdata = outputStream.toByteArray()
                (lreEngine.runInference(bytesdata) as? Results<List<Recognition>, Timings>).also {
                    inferenceResults = it
                }
            }
            inferenceResults?.let { callBack(it) }
        }

        image.close()
    }

    // We can switch analyzer in the app, need to make sure the native resources are freed
    protected fun finalize() {
        lreEngine?.closeModel()
    }
}