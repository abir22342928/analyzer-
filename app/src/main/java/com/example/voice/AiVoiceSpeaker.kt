package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.model.SignalType
import java.util.Locale

object AiVoiceSpeaker : TextToSpeech.OnInitListener {

    private const val TAG = "AiVoiceSpeaker"

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isBengaliLanguageSet = false

    var isVoiceEnabled: Boolean = true

    fun initialize(context: Context) {
        if (tts == null) {
            try {
                tts = TextToSpeech(context.applicationContext, this)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to instantiate TextToSpeech: ${e.message}")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            try {
                // Try Bangladesh Bengali first, then generic Bengali
                val bnBd = Locale("bn", "BD")
                var res = tts?.setLanguage(bnBd)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val genericBn = Locale("bn")
                    res = tts?.setLanguage(genericBn)
                    if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                        isBengaliLanguageSet = true
                    }
                } else {
                    isBengaliLanguageSet = true
                }

                tts?.setSpeechRate(0.92f)
                tts?.setPitch(1.02f)
                Log.d(TAG, "TextToSpeech successfully initialized. Bengali available: $isBengaliLanguageSet")
            } catch (e: Exception) {
                Log.e(TAG, "Error configuring TTS language: ${e.message}")
            }
        } else {
            Log.e(TAG, "TextToSpeech onInit failed with status code: $status")
        }
    }

    /**
     * Speaks the requested Bengali phrase. If the Android system lacks the Bengali voice engine data,
     * falls back to the English equivalent so voice guidance is always audible.
     */
    fun speak(bengaliText: String, fallbackEnglishText: String? = null) {
        if (!isVoiceEnabled) return
        val currentTts = tts ?: return

        try {
            if (isBengaliLanguageSet) {
                currentTts.speak(bengaliText, TextToSpeech.QUEUE_FLUSH, null, "VOICE_${System.currentTimeMillis()}")
            } else {
                // If Bengali locale data is not installed on device TTS, speak English fallback
                val spokenText = fallbackEnglishText ?: bengaliText
                currentTts.language = Locale.US
                currentTts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "VOICE_${System.currentTimeMillis()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Speech execution error: ${e.message}")
        }
    }

    /**
     * Spoken when NO trading chart is open or visible on the captured screen:
     * "আমি কোন ট্রেডিং চার্ট দেখতে পারছি না। দয়া করে আপনার কোটেক্স ট্রেডিং চার্টটি খুলুন।"
     */
    fun speakNoChartDetected() {
        speak(
            bengaliText = "আমি কোন ট্রেডিং চার্ট দেখতে পারছি না। দয়া করে আপনার কোটেক্স ট্রেডিং চার্টটি খুলুন।",
            fallbackEnglishText = "I cannot see any trading chart. Please open your Quotex trading chart."
        )
    }

    /**
     * Spoken when a trading chart is detected and scanning is ongoing:
     * "আমি এখন ট্রেডিং চার্ট দেখতে পাচ্ছি এবং এনালাইসিস করছি। পরবর্তী ক্যান্ডেল আপ বা ডাউন হতে পারে।"
     */
    fun speakChartDetectedAndAnalyzing() {
        speak(
            bengaliText = "আমি এখন ট্রেডিং চার্ট দেখতে পাচ্ছি এবং এনালাইসিস করছি। পরবর্তী ক্যান্ডেল আপ বা ডাউন হতে পারে।",
            fallbackEnglishText = "I can see the trading chart now and I am analyzing. The next candle can be UP or DOWN."
        )
    }

    /**
     * Spoken once deep AI Brain multi-factor analysis is finalized:
     * Announces exact Next Candle prediction (UP / DOWN) and Confidence Score.
     */
    fun speakPrediction(signal: SignalType, score: Int, pattern: String? = null) {
        val bengaliDir = if (signal == SignalType.UP) "আপ" else "ডাউন"
        val engDir = if (signal == SignalType.UP) "UP" else "DOWN"

        val bnText = "এনালাইসিস সম্পন্ন হয়েছে। পরবর্তী ক্যান্ডেল $bengaliDir হবে। কনফিডেন্স স্কোর $score শতাংশ।"
        val enText = "Analysis complete. Next candle will be $engDir. Confidence score $score percent."

        speak(
            bengaliText = bnText,
            fallbackEnglishText = enText
        )
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS: ${e.message}")
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS: ${e.message}")
        }
    }
}
