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
    private var callback: IRecognizerCallback? = null

    override fun setCallback(callback: IRecognizerCallback) {
        this.callback = callback
    }

    override fun create() {
        val assets = Assets(context)
        val assetDir = assets.syncAssets()
        recognizer = SpeechRecognizerSetup.defaultSetup()
            .setAcousticModel(File(assetDir, "en-us-ptm"))
            .setDictionary(File(assetDir, "cmudict-en-us.dict"))
            .setRawLogDir(assetDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
            .recognizer
        recognizer?.addListener(this)

        val keyGrammar = File(assetDir, "key.gram")
        recognizer?.addKeywordSearch(KWS_SEARCH, keyGrammar)
    }

    override fun startListening() {
        recognizer?.stop()
        recognizer?.startListening(KWS_SEARCH)
//      recognizer?.startListening(searchName, 10000)
    }

    override fun stopListening() {
        recognizer?.stop()
    }

    override fun destroy() {
        recognizer?.cancel()
        recognizer?.shutdown()
    }

    override fun onResult(hypothesis: Hypothesis?) {
        println(hypothesis?.hypstr)
        callback?.onResult(hypothesis?.hypstr)
    }

    override fun onPartialResult(hypothesis: Hypothesis?) {
        if (hypothesis == null)
            return
        val text = hypothesis.hypstr
        if (text == KWS_SEARCH) {
            println("wake up")
            callback?.onResult("")
        }
    }

    override fun onTimeout() {
        startListening()
    }

    override fun onBeginningOfSpeech() {
    }

    override fun onEndOfSpeech() {
    }

    override fun onError(p0: Exception?) {

    }

    private fun setupRecognizer(assetDir: File?) {

    }

    companion object {
        /* Named searches allow to quickly reconfigure the decoder */
        const val KWS_SEARCH = "wakeup"
        const val KEYWORD = "take photo"
    }
}