package us.nonda.aidemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var pocketsphinxRecognizer: PocketsphinxRecognizer? = null

    /* Used to handle permission request */
    private val PERMISSIONS_REQUEST_RECORD_AUDIO = 100

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
    }

    private fun initRecognizer() {
        pocketsphinxRecognizer = PocketsphinxRecognizer(baseContext)
        pocketsphinxRecognizer?.create()
        pocketsphinxRecognizer?.setCallback(object : PocketsphinxRecognizer.IPocketsphinxCallback {
            override fun onWakeUp() {
                pocketsphinxRecognizer?.stopListening()
                showResult("Im back!!!")
            }
        })
        startWakeUp()
    }

    private fun startWakeUp() {
        pocketsphinxRecognizer?.startListening()
        startDemonstration()
    }

    private fun startDemonstration() {
        tvResult.text = resources.getString(R.string.to_start_demonstration)
    }

    private fun showResult(result: String?) {
        tvResult.append(System.lineSeparator())
        tvResult.append(result)
    }

    override fun onDestroy() {
        super.onDestroy()
        pocketsphinxRecognizer?.destroy()
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

}


