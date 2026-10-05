package com.pocketrocket.nearmiss.debrief

import android.content.Context
import android.speech.tts.TextToSpeech
import com.pocketrocket.nearmiss.events.NearMissEvent
import java.util.Locale

class DebriefEngine(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.ENGLISH
        }
    }

    fun setLanguage(locale: Locale) {
        if (isInitialized) {
            tts?.language = locale
        }
    }

    fun generateSummary(events: List<NearMissEvent>): String {
        val confirmed = events.filter { it.confirmed }
        if (confirmed.isEmpty()) {
            return "Good ride. No confirmed near-misses recorded."
        }
        val closest = confirmed.minByOrNull { it.minTtc }
        val count = confirmed.size
        
        return "$count near-misses this ride. " +
               if (closest != null) {
                   "Closest was a ${closest.objectClass} at ${String.format("%.1f", closest.minTtc)} seconds."
               } else ""
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
