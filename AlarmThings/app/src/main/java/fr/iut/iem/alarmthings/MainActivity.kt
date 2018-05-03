package fr.iut.iem.alarmthings

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */

class MainActivity : Activity() {
    companion object {
        private val TAG = MainActivity.toString()

        private const val GPIO_PIN_LED = "BCM2"
        private const val GPIO_PIN_SENSOR = "BCM3"
        private const val GPIO_PIN_BUZZER = "BCM4"

        private const val IMAGE_SIZE = 400
    }

    private lateinit var ledGpio: Gpio
    private lateinit var sensorGpio: Gpio
    private lateinit var buzzerGpio: Gpio
    private val cameraManager = CameraManager.instance
    private lateinit var cameraHandler: Handler
    private lateinit var cameraThread: HandlerThread
    private var isDetected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpGpio()
        initCamera()
    }

    override fun onStart() {
        super.onStart()
        blinkLED()
        detect()
    }

    private fun setUpGpio() {
        val pioService = PeripheralManager.getInstance()
        try {
            Log.i(TAG, "Configuring GPIO pins")
            ledGpio = pioService.openGpio(GPIO_PIN_LED)
            ledGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

            sensorGpio = pioService.openGpio(GPIO_PIN_SENSOR)
            sensorGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

            buzzerGpio = pioService.openGpio(GPIO_PIN_BUZZER)
            buzzerGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

            Log.i(TAG, "Configuring successful")
        } catch (e: IOException) {
            Log.e(TAG, "Error configuring GPIO pins", e)
        }
    }

    private fun buzz() {
        val buzz = Runnable {
            Log.i(TAG, "BUUUUUUUZZZZ")
            buzzerGpio.value = true
            sleep(50)
            buzzerGpio.value = false
            Log.i(TAG, "DONT BUZZ")
        }
        Thread(buzz).start()
    }

    private fun detect() {
        val detector = Runnable {
            while (true) {
                if (sensorGpio.value && !isDetected) {
                    isDetected = true
                    Log.i(TAG, sensorGpio.value.toString())
                    //buzz()
                    runOnUiThread { cameraManager.takePicture()}
                }
                sleep(100)
            }
        }

        Thread(detector).start()
    }

    private fun blinkLED() {
        val ledBlinker = Runnable {
            while (true) {
                // Turn on the LED
                ledGpio.value = true
                sleep(1000)
                // Turn off the LED
                ledGpio.value = false
                sleep(1000)
            }
        }
        Thread(ledBlinker).start()
    }

    private fun sleep(milliseconds: Int) {
        try {
            Thread.sleep(milliseconds.toLong())
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun initCamera() {
        // Creates new handlers and associated threads for camera
        cameraThread = HandlerThread("CameraBackground")
        cameraThread.start()
        cameraHandler = Handler(cameraThread.looper)
        cameraManager.initializeCamera(this, cameraHandler, mOnImageAvailableListener)
    }

    private val mOnImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        // get image bytes
        val imageBuf = image.planes[0].buffer
        val imageBytes = ByteArray(imageBuf.remaining())
        imageBuf.get(imageBytes)
        image.close()

        bindCameraImage(imageBytes)
    }

    private fun bindCameraImage(imageBytes: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        runOnUiThread({
            this.camera_view.setImageBitmap(
                    Bitmap.createScaledBitmap(
                            bitmap,
                            IMAGE_SIZE,
                            IMAGE_SIZE,
                            false)
            )
        })
    }
}
