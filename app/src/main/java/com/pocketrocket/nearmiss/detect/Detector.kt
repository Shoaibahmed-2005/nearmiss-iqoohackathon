package com.pocketrocket.nearmiss.detect

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector

data class DetectionResult(
    val id: Int = 0,
    val className: String,
    val score: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

interface Detector {
    fun initialize(context: Context)
    fun detect(bitmap: Bitmap, timestampMs: Long): Pair<List<DetectionResult>, Long>
    fun close()
}

class MediaPipeDetector : Detector {
    private var objectDetector: ObjectDetector? = null

    override fun initialize(context: Context) {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("efficientdet-lite0.tflite")
            .build()
            
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setMaxResults(10)
            .setScoreThreshold(0.5f)
            .setCategoryAllowlist(listOf("car", "motorcycle", "bus", "truck", "bicycle", "person"))
            .build()
            
        objectDetector = ObjectDetector.createFromOptions(context, options)
    }

    override fun detect(bitmap: Bitmap, timestampMs: Long): Pair<List<DetectionResult>, Long> {
        val detector = objectDetector ?: return Pair(emptyList(), 0L)
        val mpImage = BitmapImageBuilder(bitmap).build()
        val startTime = SystemClock.elapsedRealtime()
        val results = detector.detectForVideo(mpImage, timestampMs)
        val inferenceTimeMs = SystemClock.elapsedRealtime() - startTime

        val mappedResults = results.detections().mapNotNull { det ->
            val cat = det.categories().firstOrNull() ?: return@mapNotNull null
            val box = det.boundingBox()
            DetectionResult(
                className = cat.categoryName(),
                score = cat.score(),
                left = box.left,
                top = box.top,
                right = box.right,
                bottom = box.bottom
            )
        }
        return Pair(mappedResults, inferenceTimeMs)
    }

    override fun close() {
        objectDetector?.close()
        objectDetector = null
    }
}
