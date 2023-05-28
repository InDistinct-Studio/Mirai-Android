package work.indistinct.mirai.demo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import work.indistinct.mirai.CardImage
import work.indistinct.mirai.IDCardResult
import work.indistinct.mirai.Mirai
import work.indistinct.mirai.face.FaceDetectionResult
import work.indistinct.mirai.face.FaceScreeningStage
import work.indistinct.mirai.face.FaceScreeningState
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@androidx.camera.core.ExperimentalGetImage
class MainActivity : AppCompatActivity(), Mirai.OnInitializedListener {

    lateinit var swapCameraButton: Button
    lateinit var resultText: TextView
    lateinit var confidenceText: TextView
    lateinit var faceText: TextView
    lateinit var faceDetectionSwitch: Switch
    lateinit var autoCaptFaceSwitch: Switch
    lateinit var faceStageSpinner: Spinner
    lateinit var previewView: PreviewView
    lateinit var boundingBoxOverlay: BoundingBoxOverlay
    lateinit var imageView: ImageView

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
    private var cameraProvider: ProcessCameraProvider? = null

    private var previewWidth: Int = 1
    private var previewHeight: Int = 1
    private var imgProxyWidth: Int = 1
    private var imgProxyHeight: Int = 1
    private var imgProxyRotationDegree: Int = 0
    private lateinit var cameraExecutor: ExecutorService

    private var correctCount: Int = 0
    private var faceScreeningState: FaceScreeningState = FaceScreeningState(
        stage = FaceScreeningStage.FRONT
    )

    private var selectedFaceAction: FaceScreeningStage = FaceScreeningStage.UP

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Mirai.init(this,"NDIfT5TihAj7wVZi178O", this)
        imageView = findViewById(R.id.imageView)
        boundingBoxOverlay = findViewById(R.id.boundingBoxOverlay)
        previewView = findViewById(R.id.previewView)
        resultText = findViewById(R.id.resultTextView)
        confidenceText = findViewById(R.id.confidenceTextView)
        swapCameraButton = findViewById(R.id.swapCameraButton)
        faceText = findViewById(R.id.faceTextView)
        faceDetectionSwitch = findViewById(R.id.faceDetectionSwitch)
        autoCaptFaceSwitch = findViewById(R.id.autoCapFaceSwitch)
        faceDetectionSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
            faceStageSpinner.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        faceStageSpinner = findViewById(R.id.faceStageSpinner)
        faceStageSpinner.adapter = ArrayAdapter.createFromResource(this, R.array.face_stages, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        faceStageSpinner.onItemSelectedListener = object: OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                selectedFaceAction = FaceScreeningStage.values()[pos + 1]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }



        swapCameraButton.setOnClickListener {
            swapCamera()
        }

        imageView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                imageView.setImageBitmap(null)
                correctCount = 0
                faceScreeningState = FaceScreeningState()
                cameraProvider?.unbindAll()
                bindCameraUseCases()

                return false
            }
        })

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 1000)
            }
        }
    }

    override fun onCompleted() {
        Toast.makeText(this, "Init Completed", Toast.LENGTH_LONG).show()
        cameraExecutor = Executors.newSingleThreadExecutor()
        setupCamera()
    }

    override fun onError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

//        val metrics = DisplayMetrics().also { previewView.display.getRealMetrics(it) }

//        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        previewView.scaleType = PreviewView.ScaleType.FIT_START
        val rotation = previewView.display.rotation

//        val previewChild = previewView.getChildAt(0)
        previewWidth = (previewView.width * previewView.scaleX).toInt()
        previewHeight = (previewView.height * previewView.scaleY).toInt()
        val screenAspectRatio = aspectRatio(previewWidth, previewHeight)

        preview = Preview.Builder()
            .setTargetResolution(Size(720, 1280))
            .setTargetRotation(rotation)
            .build()


        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetResolution(Size(720, 1280))
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(720, 1280))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->

                    imageProxy.run {
                        val reverseDimens = imageInfo.rotationDegrees == 90 || imageInfo.rotationDegrees == 270
                        imgProxyWidth = if (reverseDimens) imageProxy.height else imageProxy.width
                        imgProxyHeight = if (reverseDimens) imageProxy.width else imageProxy.height
                        imgProxyRotationDegree = imageInfo.rotationDegrees
                        val card = CardImage(this.image!!, imageInfo.rotationDegrees)
                        Mirai.scanIDCard(card) { result ->
                            if (faceDetectionSwitch.isChecked) {
                                Mirai.checkFaceAction(result, faceScreeningState, selectedFaceAction) { faceState ->
                                    this@MainActivity.displayResult(result, faceScreeningState.curFaceDetectionResult)
                                    imageProxy.close()
                                    faceScreeningState = faceState
                                    if (faceScreeningState.stage == FaceScreeningStage.FAILED) {
                                        faceText.text = "Face screening failed!"
                                        cameraProvider?.unbindAll()
                                    }
                                    if (faceScreeningState.stage == FaceScreeningStage.FINISH && autoCaptFaceSwitch.isChecked) {
                                        imageView.setImageBitmap(faceScreeningState.results[0].faceBitmap)
                                        cameraProvider?.unbindAll()
                                    }
                                }
                            } else {
                                this@MainActivity.displayResult(result, null)
                                imageProxy.close()
                            }
                        }
                    }
                }
            }

        cameraProvider?.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider?.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer)

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(previewView.surfaceProvider)
        } catch (exc: Exception) {
        }
    }

    private fun swapCamera() {
        cameraProvider?.unbindAll()
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCameraUseCases()
    }

    private fun displayResult(result: IDCardResult, faceResult: FaceDetectionResult?) {
        result.error?.run {
            resultText.text = errorMessage
        }
        boundingBoxOverlay.post{boundingBoxOverlay.clearBounds()}
        val greenBoxes: MutableList<Rect> = mutableListOf()
        val redBoxes: MutableList<Rect> = mutableListOf()
        var mlBBox: Rect? = null
        result.run {
            // fullImage is always available.
            val capturedImage = fullImage

            confidenceText.text = "%.3f ".format(confidence)
            if (this.detectResult != null) {
                confidenceText.text = "%.3f (%.3f) (%.3f)".format(
                    confidence,
                    this.detectResult!!.mlConfidence,
                    this.detectResult!!.boxConfidence)
                if (this.detectResult!!.cardBoundingBox != null) {
                    mlBBox = this.detectResult!!.cardBoundingBox!!.transform()
                }
            }
            if (isFrontSide != null && isFrontSide as Boolean) {
                // cropped image is only available for front side scan result.
                val cardImage = croppedImage
                confidenceText.text = "%s, Full: %s".format(confidenceText.text, isFrontCardFull)

                if (classificationResult != null && classificationResult!!.error == null) {
                    confidenceText.text = "%s (%.3f)".format(confidenceText.text, classificationResult!!.confidence)
                }
            }

            if (texts != null) {
                resultText.text = "TEXTS -> ${texts!!.joinToString("\n")}, isFrontside -> $isFrontSide"
            } else {
                resultText.text = "TEXTS -> NULL, isFrontside -> $isFrontSide"
            }
            if (idBBoxes != null) {
                if (isFrontCardFull == true && confidence > 0.5 && classificationResult!!.confidence > 0.7){
                    greenBoxes.addAll(idBBoxes!!)
                } else {
                    redBoxes.addAll(idBBoxes!!)
                }
            }
            if (cardBox != null) {
                if (isFrontCardFull == true && confidence > 0.5 && classificationResult!!.confidence > 0.7){
                    greenBoxes.add(cardBox!!)
                } else {
                    redBoxes.add(cardBox!!)
                }
            }
            if (faceBox != null) {
                if (isFrontCardFull == true && confidence > 0.5 && classificationResult!!.confidence > 0.7){
                    greenBoxes.add(faceBox!!)
                } else {
                    redBoxes.add(faceBox!!)
                }
            }
            if (faceResult != null && faceResult.error == null) {
                if (faceResult.selfieFace != null) {
                    if (faceResult.selfieFace!!.isFrontFacing && faceResult.selfieFace!!.isFullFace && faceResult.selfieFace!!.isGoodSize) {
                        greenBoxes.add(faceResult.selfieFace!!.bbox)
                    } else {
                        redBoxes.add(faceResult.selfieFace!!.bbox)
                    }
                    val rotX = faceResult.selfieFace!!.rot.rotX
                    val rotY = faceResult.selfieFace!!.rot.rotY
                    val rotZ = faceResult.selfieFace!!.rot.rotZ
                    val faceWidth = faceResult.selfieFace!!.faceBitmap?.width
                    val faceHeight = faceResult.selfieFace!!.faceBitmap?.height
                    faceText.text =
                        "Stage: ${faceScreeningState.stage} Num: ${faceResult.faceScreeningResults!!.size}, Full: ${faceResult.selfieFace!!.isFullFace}, Front: ${faceResult.selfieFace!!.isFrontFacing} (%.1f, %.1f, %.1f) Face Size: %d, %d (%d, %d)".format(
                            rotX, rotY, rotZ, faceWidth, faceHeight, result.fullImage?.width ?: -1, result.fullImage?.height ?: -1
                        )

                } else {
                    faceText.text = ""
                }
            }
            boundingBoxOverlay.post{boundingBoxOverlay.drawBounds(
                greenBoxes.map{it.transform()},
                redBoxes.map{it.transform()},
                mlBBox,
                imgProxyRotationDegree)}
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }


    private fun Rect.transform(): Rect {

        var scale = previewWidth / imgProxyWidth.toFloat()
        if (imgProxyRotationDegree == 0 || imgProxyRotationDegree == 180) {

            scale = previewHeight / imgProxyHeight.toFloat()
        }


        var flippedLeft = left
        var flippedRight = right
        if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            flippedLeft = imgProxyWidth - right
            flippedRight = imgProxyWidth - left
        }

        // Scale all coordinates to match preview
        val scaledLeft = scale * flippedLeft
        val scaledRight = scale * flippedRight
        val scaledTop = scale * top
        val scaledBottom = scale * bottom
        return Rect(scaledLeft.toInt(), scaledTop.toInt(), scaledRight.toInt(), scaledBottom.toInt())
    }
}
