package com.pocketrocket.nearmiss.track

import com.pocketrocket.nearmiss.detect.DetectionResult
import kotlin.math.max
import kotlin.math.min

class Track(
    var id: Int,
    var box: DetectionResult,
    var missedFrames: Int = 0,
    var active: Boolean = true
)

class IoUTracker(private val maxMissedFrames: Int = 3, private val iouThreshold: Float = 0.3f) {
    private var nextId = 1
    private val tracks = mutableListOf<Track>()

    fun update(detections: List<DetectionResult>): List<Track> {
        val currentTracks = tracks.filter { it.active }.toMutableList()
        val assignedDetections = BooleanArray(detections.size)
        val assignedTracks = BooleanArray(currentTracks.size)

        // Greedy matching
        for (i in detections.indices) {
            val det = detections[i]
            var bestIou = iouThreshold
            var bestTrackIdx = -1

            for (j in currentTracks.indices) {
                if (assignedTracks[j]) continue
                val track = currentTracks[j]
                if (track.box.className != det.className) continue
                
                val iou = calculateIoU(det, track.box)
                if (iou > bestIou) {
                    bestIou = iou
                    bestTrackIdx = j
                }
            }

            if (bestTrackIdx != -1) {
                assignedDetections[i] = true
                assignedTracks[bestTrackIdx] = true
                val track = currentTracks[bestTrackIdx]
                track.box = det.copy(id = track.id)
                track.missedFrames = 0
            }
        }

        // Unassigned detections become new tracks
        for (i in detections.indices) {
            if (!assignedDetections[i]) {
                val newDet = detections[i].copy(id = nextId++)
                tracks.add(Track(newDet.id, newDet, 0, true))
            }
        }

        // Unassigned tracks age
        for (j in currentTracks.indices) {
            if (!assignedTracks[j]) {
                val track = currentTracks[j]
                track.missedFrames++
                if (track.missedFrames > maxMissedFrames) {
                    track.active = false
                }
            }
        }

        tracks.removeAll { !it.active }
        return tracks.toList()
    }

    private fun calculateIoU(a: DetectionResult, b: DetectionResult): Float {
        val xA = max(a.left, b.left)
        val yA = max(a.top, b.top)
        val xB = min(a.right, b.right)
        val yB = min(a.bottom, b.bottom)

        val interArea = max(0f, xB - xA) * max(0f, yB - yA)
        if (interArea == 0f) return 0f

        val boxAArea = (a.right - a.left) * (a.bottom - a.top)
        val boxBArea = (b.right - b.left) * (b.bottom - b.top)

        return interArea / (boxAArea + boxBArea - interArea)
    }
}
