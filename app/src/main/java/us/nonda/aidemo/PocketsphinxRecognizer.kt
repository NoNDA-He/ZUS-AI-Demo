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
    private var hasWakeUp = false

    override fun create() {
        val assets = Assets(context)
        val assetDir = assets.syncAssets()
        recognizer = SpeechRecognizerSetup.defaultSetup()
            .setAcousticModel(File(assetDir, "cn-ptm"))
            .setDictionary(File(assetDir, "zus-cn.dic"))
//            .setAcousticModel(File(assetDir, "en-us-ptm"))
//            .setDictionary(File(assetDir, "cmudict-en-us.dict"))
//            .setRawLogDir(assetDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
            .recognizer
        recognizer?.addListener(this)

        recognizer?.addKeyphraseSearch(KWS_SEARCH, KEYWORD)
        val keyGrammar = File(assetDir, "key.gram")
        recognizer?.addKeywordSearch(KWS_SEARCH, keyGrammar)

        val languageModel = File(assetDir, "zus-cn-mode.lm")
        recognizer?.addNgramSearch(KWS_SEARCH, languageModel)

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
        print(hypothesis.hypstr)
        if (hasWakeUp) return
        handleResult(hypothesis.hypstr)
    }

    private fun handleResult(text: String?) {
        if (text == null) return
        for (s in KEYWORD_LIST) {
            if (text.contains(s)) {
                stopListening()
                callback?.onWakeUp()
            }
        }
        checkResultIfNeedRestart(text)
        callback?.showLog("result is $text")
    }

    override fun onTimeout() {
        if (!hasWakeUp) startListening()
    }

    override fun onBeginningOfSpeech() {
        callback?.showLog("begin speech")
    }

    override fun onEndOfSpeech() {
        callback?.showLog("end speech")
    }

    override fun onError(e: Exception?) {
        callback?.showLog("onError ${e.toString()}")
    }

    private fun checkResultIfNeedRestart(result: String) {
        val wordSize = result.split(" ").size
        if (wordSize > MAX_RECOGNIZED_WORD) {
            callback?.restartIfNeeded()
        }
    }

    interface IPocketsphinxCallback {
        fun onWakeUp()
        fun showLog(log: String)
        fun restartIfNeeded()
    }

    companion object {
        /* Named searches allow to quickly reconfigure the decoder */
        const val KWS_SEARCH = "wakeup"
        val KEYWORD_LIST = arrayListOf("祖斯", "租斯", "嗨 祖斯")
        const val KEYWORD = "祖斯"
        const val MAX_RECOGNIZED_WORD = 3
    }
}