package com.camera_with_mlkit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Choreographer
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.android.synthetic.main.text_camera_preview.view.*
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (Double) -> Unit
typealias TextListener = (texts: Text) -> Unit

class TextCameraPreview(val reactContext: ReactContext, val activity: Activity?) : LinearLayout(reactContext) {

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    init {
        View.inflate(reactContext, R.layout.text_camera_preview, this)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Request permission
        val fulfilled = allPermissionsGranted()
        Log.d(TAG, "permission fulfilled: $fulfilled")
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            activity?.let {
                ActivityCompat.requestPermissions(
                    it,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }

        // sset up listeners
//        camera_capture_button.setOnClickListener { takePhoto() }
        setupLayoutHack()
    }

    fun setupLayoutHack() {
        Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                manuallyLayoutChildren()
                viewTreeObserver.dispatchOnGlobalLayout()
                Choreographer.getInstance().postFrameCallback(this)
            }
        })
    }

    fun manuallyLayoutChildren() {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY))
            child.layout(0, 0, child.measuredWidth, child.measuredHeight)
        }
    }

    private fun takePhoto() {
        // get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        // create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object: ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

    fun onReceiveNativeEvent(recognizedText: Text) {
        val blocks = recognizedText.textBlocks
        if (blocks.size == 0) return

        val allTexts = blocks.flatMap { it.lines.flatMap { it.elements.map { it.text } } }

        val event: WritableMap = Arguments.createMap()
        val texts: WritableArray = Arguments.fromList(allTexts)

        event.putArray("texts", texts)

        reactContext.getJSModule(RCTEventEmitter::class.java).receiveEvent(getId(), "recognized", event)
    }

    fun startCamera() {

        val ver = Build.VERSION.SDK_INT
        Log.d(TAG, "startCamera: $ver")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(reactContext)

            Log.d(TAG, "cameraProvider addListener")
            // used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider:ProcessCameraProvider = cameraProviderFuture.get()

            // preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(cameraExecutor, TextAnalyzer { texts ->
                    Log.d(TAG, "Average text: ${texts.text}")
                    onReceiveNativeEvent(texts)
                })
            }

            // select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(activity as LifecycleOwner , cameraSelector, preview, imageCapture, imageAnalyzer )

            }
            catch(exc:Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            context, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()
            val data = ByteArray(remaining())
            get(data)
            return data
        }

        override fun analyze(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }

    }


    private class TextAnalyzer(listener:TextListener? = null) : ImageAnalysis.Analyzer {
        private val listeners = ArrayList<TextListener>().apply { listener?.let {add(it)} }

        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            if (listeners.isEmpty()) {
                imageProxy.close()
                return
            }

            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                val recognizer = TextRecognition.getClient()
                recognizer.process(image).addOnSuccessListener {text ->
                    listeners.forEach{ it(text)}
                    imageProxy.close()
                }.addOnFailureListener {
                    imageProxy.close()
                    it.printStackTrace()
                }
            }

        }

    }
}