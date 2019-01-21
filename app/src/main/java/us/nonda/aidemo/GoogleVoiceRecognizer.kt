package us.nonda.aidemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.*

/**
 * Created by yanghe on 2019/1/15.
 * <p>
 */
class GoogleVoiceRecognizer(private val context: Context) : IRecognizer, RecognitionListener {

    private var mSpeechRecognizer: SpeechRecognizer? = null
    var callback: IGoogleVoiceCallback? = null
    var isWorking = false

    override fun create() {
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        mSpeechRecognizer?.setRecognitionListener(this)
    }

    override fun startListening() {
        if (isWorking) return
        val intent = Intent(RecognizerIntent.EXTRA_CALLING_PACKAGE)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        mSpeechRecognizer?.startListening(intent)
    }

    override fun stopListening() {
        isWorking = false
        mSpeechRecognizer?.cancel()
    }

    override fun destroy() {
        if (isWorking) {
            mSpeechRecognizer?.cancel()
        }
        mSpeechRecognizer?.destroy()
    }

    override fun onReadyForSpeech(params: Bundle?) {
    }

    override fun onRmsChanged(rmsdB: Float) {
    }

    override fun onBufferReceived(buffer: ByteArray?) {
    }

    override fun onPartialResults(results: Bundle?) {
        if (results == null) return
//        val list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//        when (list) {
//            null -> print("no result")
//            else ->
//                if (!list.isEmpty()) callback?.onResult(list[0])
//                else print("no result")
//        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
    }

    override fun onBeginningOfSpeech() {
    }

    override fun onEndOfSpeech() {
    }

    override fun onError(error: Int) {
        callback?.onFinish()
    }

    override fun onResults(results: Bundle?) {
        if (results == null) return
        val list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        when (list) {
            null -> print("no result")
            else ->
                if (!list.isEmpty()) callback?.onResult(list[0])
                else print("no result")
        }
        callback?.onFinish()
    }

    interface IGoogleVoiceCallback {
        fun onResult(result: String)
        fun onFinish()
    }

}