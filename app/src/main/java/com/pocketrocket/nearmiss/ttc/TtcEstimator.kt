package com.pocketrocket.nearmiss.ttc

import com.pocketrocket.nearmiss.track.Track
import kotlin.math.max

class TtcEstimator(private val emaAlpha: Float = 0.3f, private val maxTtc: Float = 10f) {
    
    private val trackHeights = mutableMapOf<Int, SmoothedHeight>()

    data class SmoothedHeight(
        var value: Float,
        var lastTimeMs: Long
    )

    fun estimateTtc(track: Track, timestampMs: Long): Float? {
        val h = track.box.bottom - track.box.top
        val prev = trackHeights[track.id]
        
        if (prev == null) {
            trackHeights[track.id] = SmoothedHeight(h, timestampMs)
            return null
        }
        
        val dt = (timestampMs - prev.lastTimeMs) / 1000f
        if (dt <= 0f) return null

        val smoothedH = emaAlpha * h + (1 - emaAlpha) * prev.value
        val dh = smoothedH - prev.value
        
        trackHeights[track.id] = SmoothedHeight(smoothedH, timestampMs)
        
        val dhdt = dh / dt
        if (dhdt <= 0.1f) {
            // Box is not growing, or growing too slowly
            return null
        }
        
        val ttc = smoothedH / dhdt
        return minOf(max(0f, ttc), maxTtc)
    }

    fun cleanUp(activeTrackIds: Set<Int>) {
        val toRemove = trackHeights.keys.filter { it !in activeTrackIds }
        toRemove.forEach { trackHeights.remove(it) }
    }
}
