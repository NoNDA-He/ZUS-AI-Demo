package us.nonda.aidemo

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.*
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
    private var takePhoto = false

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
//        testUSB()
    }

    private fun initRecognizer() {
        pocketsphinxRecognizer = PocketsphinxRecognizer(baseContext)
        pocketsphinxRecognizer?.create()
        pocketsphinxRecognizer?.callback = (object : PocketsphinxRecognizer.IPocketsphinxCallback {
            override fun onWakeUp() {
                showResult("What can I do for you?")
                googleVoiceRecognizer?.startListening()
                takeCheck = false
                takePhoto = false
                waveView.visibility = View.VISIBLE
                waveView.startAnim()
                tvTips.visibility = View.INVISIBLE
            }

            override fun showLog(log: String) {
                showLogResult(log)
            }

            override fun restartIfNeeded() {
                showLogResult("Restart Pocketsphinx")
                pocketsphinxRecognizer?.stopListening()
                window.decorView.handler.postDelayed({ startWakeUp() }, 100)
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
        firstStartWakeUp()
    }

    private fun firstStartWakeUp() {
        startWakeUp()
        showResult(resources.getString(R.string.init_pocketsphinx))
        Handler().postDelayed({ startWakeUp() }, 3000)
    }

    private fun startWakeUp() {
        googleVoiceRecognizer?.stopListening()
        pocketsphinxRecognizer?.startListening()
        showResult(resources.getString(R.string.to_start_demonstration))
        waveView.stopAnim()
        waveView.visibility = View.INVISIBLE
        tvTips.visibility = View.VISIBLE
    }

    private fun recordResult(result: String) {
//        showLogResult(result)
        if (result.contains("have") && result.contains("check")) {
            takeCheck = true
        } else if (result.contains("take") && result.contains("photo")) {
            takePhoto = true
        }
    }

    private fun handleResult() {
        when {
            takeCheck -> {
                Toast.makeText(baseContext, "Ok, I will take a check", Toast.LENGTH_LONG).show()
                showResult("Ok, I will have a check")
            }
            takePhoto -> {
                Toast.makeText(baseContext, "Ok, I will take a photo", Toast.LENGTH_LONG).show()
                showResult("Ok, I will take a photo")
            }
            else -> showResult("Sorry, has no result")
        }
        takeCheck = false
        takePhoto = false
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
        reading = false
        waveView.release()
        if (usbPermissionReceiver != null) {
            unregisterReceiver(usbPermissionReceiver)
        }
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

    /////////////////getAudioDevice/////////

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
            22 -> "TYPE_USB_HEADSET"
            else -> "unknown type"
        }
    }

    var manager: UsbManager? = null
    var usbDevice: UsbDevice? = null
    var usbPermissionReceiver: USBPermissionReceiver? = null



    ////////////////////////////////get usb info////////
    // vid = 8137  pid = 152
    private fun testUSB() {

        manager = getSystemService(Context.USB_SERVICE) as UsbManager
        for (device in manager?.deviceList?.values!!) {
            if (device.vendorId == 8137 && device.productId == 152) {
                usbDevice = device
                showLogResult(usbDevice?.deviceName!!)
            }
        }
        if (usbDevice == null) return
        val interfaceCount = usbDevice?.interfaceCount
        showLogResult("interfaceCount = $interfaceCount")

        if (manager?.hasPermission(usbDevice)!!) {
            startReadData()
        } else {
            showLogResult("no permission")
            requestUSBPermission()
        }
    }

    private fun startReadData() {
        reading = true
        Thread(ReadThread(manager, usbDevice)).start()
    }

    private fun requestUSBPermission() {
        usbPermissionReceiver = USBPermissionReceiver()
        val intent = Intent(ACTION_DEVICE_PERMISSION)
        val pendingIntent = PendingIntent.getBroadcast(baseContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val intentFilter = IntentFilter(ACTION_DEVICE_PERMISSION)
        baseContext.registerReceiver(usbPermissionReceiver, intentFilter)
        manager?.requestPermission(usbDevice, pendingIntent)
    }

    inner class USBPermissionReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == ACTION_DEVICE_PERMISSION) {
                synchronized(this) {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (usbDevice?.deviceName == device.deviceName) {
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            startReadData()
                        } else {
                            showLogResult("granted permission failed")
                        }
                    }
                }
            }
        }
    }

    var reading = false

    inner class ReadThread(var manager: UsbManager?, var usbDevice: UsbDevice?) : Runnable {

        var usbEndpointIn: UsbEndpoint? = null
        var usbDeviceConnection: UsbDeviceConnection? = null

        override fun run() {
            while (reading) {
                openUSBDevice()
                Thread.sleep(1000)
            }
        }

        private fun openUSBDevice() {
            if (usbDevice?.interfaceCount == 0) return
            val usbInterface = usbDevice?.getInterface(6)
            println("endpointCount = ${usbInterface?.endpointCount}")
//            for (i in 0 until usbInterface?.endpointCount!!) {
                if (usbInterface?.endpointCount!! >= 1) {
                    val endpoint = usbInterface.getEndpoint(0)
//                    if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK
//                        && endpoint.direction == UsbConstants.USB_DIR_IN
//                    ) {
                        usbEndpointIn = endpoint
                        println("init usbEndpointIn")
//                    }
                }
//            }
            usbDeviceConnection = manager?.openDevice(usbDevice)
            if (usbDeviceConnection != null) {
                val claimInterface = usbDeviceConnection!!.claimInterface(usbInterface, true)
                println(claimInterface.toString())
                readUSBData()
            } else {
                println("connection null")
            }
        }

        private fun readUSBData() {
            if (usbEndpointIn != null) {
                val bytes = ByteArray(64)
                val ret = usbDeviceConnection?.bulkTransfer(usbEndpointIn, bytes, bytes.size, 1000)
                if (ret != null) {
                    if (ret > 2) {
                        println("read success")
                    } else {
                        println("read failed")
                    }
                } else {
                    println("read failed")
                }
            }
        }

    }

    companion object {
        const val ACTION_DEVICE_PERMISSION = "ACTION_DEVICE_PERMISSION"
    }

}


