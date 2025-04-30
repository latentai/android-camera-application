package io.latentai.android.lre.sample.processor

import android.graphics.RectF
import io.latentai.android.lre.inference.InferenceOptions
import io.latentai.android.lre.model.LREMetaData
import io.latentai.android.lre.model.Model
import io.latentai.android.lre.processor.BoundingBox
import io.latentai.android.lre.processor.InferPostprocessor
import io.latentai.android.lre.processor.Recognition
import java.util.StringTokenizer

/**
 * Postprocessing class implemented by developer to post process inference outputs
 * to designed Recognition
 * For this example, the recognition results are confidence score, object label and bounding box
 */
class DetectorMnetNMSPostprocessor(model: Model?) : InferPostprocessor(model) {

    val confidence_threshold = 0.5f
    val outputshape = LREMetaData.getOutputShapes()

    val candidates = getModelCandidates(outputshape.toString())  // Get number of candidates from model metadata
    val classes = getModelClasses(outputshape.toString())
    /**
     * Converts multiple outputs (from model outputshape) to a set of recognitions
     *
     * @param outputs
     * the multiple outputs being processed.
     * @return
     * a list of [Recognition] values.
     */
    override fun <T : Any?> run(
        options: InferenceOptions?
    ): List<T?>? {
        // list to store all found recognitions
        val recognitions: MutableList<Recognition> = ArrayList()

        // Convert tensors outputs
        // based on model deploy_manifest.json "output_ctx" to process outputs data

        //First set of output is bounding box data
        val rect_boxes_tensor = castToFloatArray(getInferenceOutputAt(0) as T)
        // Second set of output is label data
        val op_labels_tensor = castToFloatArray(getInferenceOutputAt(1) as T)
        // Third set of output is confidence score data
        val op_scores_tensor = castToFloatArray(getInferenceOutputAt(2) as T)

        // loop through candidates to get detection info
        for (i in 0 until candidates) {
            val x1 = rect_boxes_tensor!![i * classes + 0] * LREMetaData.getInputHeight()
            val y1 = rect_boxes_tensor[i * classes + 1] * LREMetaData.getInputWidth()
            val x2 = rect_boxes_tensor[i * classes + 2] * LREMetaData.getInputHeight()
            val y2 = rect_boxes_tensor[i * classes + 3] * LREMetaData.getInputWidth()
            val labelIdx = op_labels_tensor!![i].toInt()
            val confidence = op_scores_tensor!![i]

            if (confidence < confidence_threshold) {
                continue
            }

            var label = ""
            if (labelIdx > -1) {
                label = model.labels[labelIdx]
            } else {
                continue
            }

            if (options != null) {
                if (!options.isIncludedClass(label)) {
                    continue
                }
            }

            val recognition = Recognition(
                label,
                confidence,
                BoundingBox(
                    RectF(
                        y1,
                        x1,
                        y2,
                        x2
                    )
                )
            )
            // add each candidate to the recongnition list
            recognitions.add(recognition)
        }

        return recognitions as MutableList<T>
    }

    fun <T> castToFloatArray(value: T): FloatArray? {
        return when (value) {
            is FloatArray -> value
            is Array<*> -> {
                value.filterIsInstance<Number>().map { it.toFloat() }.toFloatArray()
            }
            is List<*> -> {
                value.filterIsInstance<Number>().map { it.toFloat() }.toFloatArray()
            }
            else -> null
        }
    }

    // Utility methods to parse outputshapes string
    // get model classes and candidates
    private fun getModelCandidates(outputShape: String): Int {
        val tokenizer = getOutputShapeTokenizer(outputShape)
        var index = 0
        while (tokenizer.hasMoreTokens()) {
            val token = tokenizer.nextToken()
            if(index == 1) {
                return token.toInt()
            }
            index++
        }
        return 0
    }

    private fun getModelClasses(outputShape: String): Int {
        val tokenizer = getOutputShapeTokenizer(outputShape)
        var index = 0
        while (tokenizer.hasMoreTokens()) {
            val token = tokenizer.nextToken()
            if(index == 2) {
                return token.toInt()
            }
            index++
        }
        return 0
    }

    private fun getOutputShapeTokenizer(outputShape: String): StringTokenizer {
        val delimiters = " []()," // Delimiters include space, [, ], (, \, and )
        return StringTokenizer(outputShape, delimiters)
    }
}
