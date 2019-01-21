package us.nonda.aidemo

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var pocketsphinxRecognizer: PocketsphinxRecognizer? = null
    private var googleVoiceRecognizer: GoogleVoiceRecognizer? = null

    /* Used to handle permission request */
    private val PERMISSIONS_REQUEST_RECORD_AUDIO = 100
    private var takeCheck = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Check if user has given permission to record audio
        val permissionCheck = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSIONS_REQUEST_RECORD_AUDIO
            )
            return
        }
        initRecognizer()
        firstStartWakeUp()
    }

    private fun initRecognizer() {
        pocketsphinxRecognizer = PocketsphinxRecognizer(baseContext)
        pocketsphinxRecognizer?.create()
        pocketsphinxRecognizer?.callback = (object : PocketsphinxRecognizer.IPocketsphinxCallback {
            override fun onWakeUp() {
                showResult("What I can do for you?")
                googleVoiceRecognizer?.startListening()
                takeCheck = false
                waveView.visibility = View.VISIBLE
                waveView.startAnim()
            }

            override fun showLog(log: String) {
                showLogResult(log)
            }

            override fun restartIfNeeded() {
                showLogResult("Restart Pocketsphinx")
                pocketsphinxRecognizer?.stopListening()
                window.decorView.handler.postDelayed({startWakeUp()}, 100)
            }
        })
        googleVoiceRecognizer = GoogleVoiceRecognizer(baseContext)
        googleVoiceRecognizer?.create()
        googleVoiceRecognizer?.callback = (object : GoogleVoiceRecognizer.IGoogleVoiceCallback {
            override fun onResult(result: String) {
                recordResult(result)
            }

            override fun onFinish() {
                handleResult()
            }
        })
    }

    private fun firstStartWakeUp() {
        startWakeUp()
        showResult(resources.getString(R.string.init_pocketsphinx))
        Handler().postDelayed({startWakeUp()}, 3000)
    }

    private fun startWakeUp() {
        googleVoiceRecognizer?.stopListening()
        pocketsphinxRecognizer?.startListening()
        showResult(resources.getString(R.string.to_start_demonstration))
        waveView.stopAnim()
        waveView.visibility = View.INVISIBLE
    }

    private fun recordResult(result: String) {
        showLogResult(result)
        if (result.contains("have") && result.contains("check")) {
            takeCheck = true
        }
    }

    private fun handleResult() {
        if (takeCheck) {
            Toast.makeText(baseContext, "Ok, I will take a check", Toast.LENGTH_LONG).show()
            showResult("ok, I will take a check")
        } else {
            showResult("Sorry...")
        }
        takeCheck = false
        window.decorView.handler.postDelayed({ startWakeUp() }, 2000)
    }

    private fun showResult(result: String?) {
        tvResult.text = result
    }

    private fun showLogResult(log: String) {
        tvLog.append(log)
        tvLog.append(System.lineSeparator())
    }

    override fun onDestroy() {
        super.onDestroy()
        waveView.release()
        pocketsphinxRecognizer?.destroy()
        pocketsphinxRecognizer = null
        googleVoiceRecognizer?.destroy()
        googleVoiceRecognizer = null
        window.decorView.handler.removeCallbacksAndMessages(null)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initRecognizer()
            } else {
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
//        getAudioDevice()
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun getAudioDevice() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_INPUTS)
        showLogResult("devices count = ${devices?.size}")
        devices?.forEach { device -> print(device) }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun print(device: AudioDeviceInfo) {
        showLogResult("device type is ${getType(device.type)}")
    }

    private fun getType(type: Int): String {
        return when (type) {
            3 -> "TYPE_WIRED_HEADSET"
            15 -> "TYPE_BUILTIN_MIC"
            18 -> "TYPE_TELEPHONY"
            else -> "unknown type"
        }
    }


}


