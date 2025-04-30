package io.latentai.android.lre.sample

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.latentai.android.lre.inference.DataType
import io.latentai.android.lre.inference.InferenceEngine
import io.latentai.android.lre.inference.InferenceOptions
import io.latentai.android.lre.model.LREMetaData
import io.latentai.android.lre.processor.Recognition
import io.latentai.android.lre.result.Results
import io.latentai.android.lre.result.Results.Timings
import io.latentai.android.lre.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.String.format
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val backgroundExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var previewWidth: Int = 0
    private var previewHeight: Int = 0

    private var inferenceEngine: InferenceEngine? = null
    private var inferenceOptions: InferenceOptions? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // init inference engine with inference options
        inferenceOptions = InferenceOptions()
        // LRE key and password are for encrypted model
        // Developers can have your own way to manage LRE key and password
        //inferenceOptions!!.lreKey = BuildConfig.LRE_KEY
        //inferenceOptions!!.lrePasswd = BuildConfig.LRE_PASSWD
        //inferenceOptions!!.device = "DLDeviceType::kDLCPU"

        // Make sure model name is the same as assets/models/{model_name}
        // you want to deploy for this app
        inferenceOptions!!.modelName = "Kaggle_detector"
        //inferenceOptions!!.modelName = "Mobile_Vit"
        // switch the data type to try different input data type
        inferenceOptions!!.dataType = DataType.IMG_BITMAP
        //inferenceOptions!!.dataType = DataType.IMG_BYTES
        inferenceEngine = InferenceEngine(this, inferenceOptions)


        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // determine rotation based on device screen size, phone = portrait, tablet = landscape
        val layout = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        val rotation = if (layout < Configuration.SCREENLAYOUT_SIZE_LARGE
        ) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // lock rotation to avoid re-creation
        setContentView(view)
        requestedOrientation = rotation

        // Request Camera permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }


            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            imageAnalysis = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            setLREAnalyzer()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
        inferenceEngine?.closeModel()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }

        }
    }

    private fun updateUI(result: Results<List<Recognition>, Timings>) {
        if (result.recognitions<Recognition>().isEmpty())
            return

        val detectionResultsMap = mutableMapOf<String, RectF>()

        runOnUiThread {
            for (index in result.recognitions<Recognition>().indices) {
                val recognition = result.recognitions<Recognition>()[index]
                binding.inferenceConfidence.text =
                    format("%.2f", (result.recognitions<Recognition>()[0].confidence * 100))
                binding.percentMeter.progress =
                    (result.recognitions<Recognition>()[0].confidence * 100).toInt()

                val detectionBox = RectF(
                    recognition.boundingBox?.x()!!,
                    recognition.boundingBox!!.y(),
                    recognition.boundingBox!!.w(),
                    recognition.boundingBox!!.h()
                )
                val detectionBounds = detectionBox.transform(
                    LREMetaData.getInputWidth(),
                    LREMetaData.getInputHeight()
                )

                // create result label
                val label = recognition.label
                val resultLabel = String.format(
                    Locale.getDefault(),
                    "%s %.2f", label,
                    100 * recognition.confidence
                )
                // using label as the key, bounding box the value
                detectionResultsMap.put(resultLabel, detectionBounds)
            }

            // Draw detected objects label and bounds
            if (detectionResultsMap.size > 0) {
                binding.detectionBoundsOverlay.post {
                    binding.detectionBoundsOverlay.drawLabelAndBounds(
                        detectionResultsMap
                    )
                }
            }
            binding.preprocessTimeValue.text =
                (Math.abs(result.timings().preprocessTime)).toString() + " ms"
            binding.postprocessTimeValue.text =
                (Math.abs(result.timings().postprocessTime)).toString() + " ms"
            binding.inferenceTimeValue.text =
                (Math.abs(result.timings().inferenceTime)).toString() + " ms"
        }
    }

    // Having the ImageAnalysis.Analyzer to invoke LRE InferenceEngine session
    // update UI with detected results
    // This is running in background to make sure UI is not blocked
    private fun setLREAnalyzer() {
        scope.launch {
            fetchPreview()
            imageAnalysis?.clearAnalyzer()
            imageAnalysis?.setAnalyzer(
                backgroundExecutor,
                LREAnalyzer(inferenceEngine, ::updateUI)
            )
        }
    }

    suspend fun fetchPreview() {
        // to get preview width and height
        delay(1000)
        val previewSize = binding.viewFinder.getChildAt(0)
        if (previewSize != null) {
            var width = previewSize.width * previewSize.scaleX
            var height = previewSize.height * previewSize.scaleY
            val rotation = previewSize.display.rotation
            if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                val temp = width
                width = height
                height = temp
            }
            previewWidth = width.toInt()
            previewHeight = height.toInt()
        }
    }

    private fun RectF.transform(width: Int, height: Int): RectF {
        val scaleX = previewWidth / width.toFloat()
        val scaleY = previewHeight / height.toFloat()

        // Scale all coordinates to match preview
        val scaledLeft = scaleX * left
        val scaledTop = scaleY * top
        val scaledRight = scaleX * right
        val scaledBottom = scaleY * bottom
        return RectF(scaledLeft, scaledTop, scaledRight, scaledBottom)
    }

    companion object {
        const val TAG = "LREAndroidSample"
        private const val REQUEST_CODE_PERMISSIONS = 100
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}