package us.nonda.aidemo

import android.content.Context
import edu.cmu.pocketsphinx.*
import java.io.File

/**
 * Created by yanghe on 2019/1/14.
 * <p>
 */
class PocketsphinxRecognizer(val context: Context) : IRecognizer, RecognitionListener {

    private var recognizer: SpeechRecognizer? = null
    var callback: IPocketsphinxCallback? = null
    var hasWakeUp = false

    override fun create() {
        val assets = Assets(context)
        val assetDir = assets.syncAssets()
        recognizer = SpeechRecognizerSetup.defaultSetup()
            .setAcousticModel(File(assetDir, "en-us-ptm"))
            .setDictionary(File(assetDir, "cmudict-en-us.dict"))
            .setRawLogDir(assetDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
            .recognizer
        recognizer?.addListener(this)

        recognizer?.addKeyphraseSearch(KWS_SEARCH, KEYWORD)
        val keyGrammar = File(assetDir, "key.gram")
        recognizer?.addKeywordSearch(KWS_SEARCH, keyGrammar)
    }

    override fun startListening() {
        hasWakeUp = false
        recognizer?.stop()
        recognizer?.startListening(KWS_SEARCH)
//      recognizer?.startListening(searchName, 10000)
    }

    override fun stopListening() {
        hasWakeUp = true
        recognizer?.stop()
    }

    override fun destroy() {
        recognizer?.cancel()
        recognizer?.shutdown()
    }

    override fun onResult(hypothesis: Hypothesis?) {
        if (hypothesis == null) return
        if (hasWakeUp) return
        handleResult(hypothesis.hypstr)
    }

    override fun onPartialResult(hypothesis: Hypothesis?) {
        if (hypothesis == null) return
        if (hasWakeUp) return
        handleResult(hypothesis.hypstr)
    }

    private fun handleResult(text: String?) {
        if (text == null) return
        if (text.contains(KEYWORD)) {
            stopListening()
            callback?.onWakeUp()
        }
    }

    override fun onTimeout() {
        if (!hasWakeUp) startListening()
    }

    override fun onBeginningOfSpeech() {
    }

    override fun onEndOfSpeech() {
    }

    override fun onError(e: Exception?) {
    }

    interface IPocketsphinxCallback {
        fun onWakeUp()
    }

    companion object {
        /* Named searches allow to quickly reconfigure the decoder */
        const val KWS_SEARCH = "wakeup"
        const val KEYWORD = "zeus"
    }
}