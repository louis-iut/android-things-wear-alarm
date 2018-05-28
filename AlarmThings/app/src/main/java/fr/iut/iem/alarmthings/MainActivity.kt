package fr.iut.iem.alarmthings

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.util.Log
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import com.google.firebase.database.*
import fr.iut.iem.alarmthings.manager.CameraManager
import fr.iut.iem.alarmthings.module.Buzzer
import fr.iut.iem.alarmthings.module.Detector
import fr.iut.iem.alarmthings.module.DetectorListener
import fr.iut.iem.alarmthings.module.Led
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

class MainActivity : Activity(), DetectorListener {

    companion object {
        private val TAG = MainActivity.toString()

        //Gpio pins
        private const val GPIO_PIN_LED = "BCM2"
        private const val GPIO_PIN_SENSOR = "BCM3"
        private const val GPIO_PIN_BUZZER = "BCM4"

        //Firebase references
        private const val ATTACK_REF = "attack"
        private const val DETECTOR_ACTIVATED_REF = "activated"
        private const val IMAGE_NAME_REF = "imageName"

        //Misc
        private const val CAMERA_THREAD_NAME = "CameraBackground"

        private const val IMAGE_SIZE = 400
    }

    //Gpio pins
    private lateinit var ledGpio: Gpio
    private lateinit var sensorGpio: Gpio
    private lateinit var buzzerGpio: Gpio

    //Camera
    private lateinit var cameraHandler: Handler
    private lateinit var cameraThread: HandlerThread

    //Modules
    private lateinit var detector: Detector
    private lateinit var buzzer: Buzzer
    private lateinit var led: Led

    //Firebase var
    private lateinit var attackRef: DatabaseReference
    private lateinit var detectorActivatedRef: DatabaseReference

    //Managers
    private val cameraManager = CameraManager.instance
    private var database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initCamera()
        setUpGpio()
        setUpModules()
        setUpFirebase()
    }

    //Not on UI Thread
    override fun onDetect() {
        Log.i(TAG, "Unknow person detected")
        runOnUiThread { cameraManager.takePicture() }
    }

    private fun setUpGpio() {
        val pioService = PeripheralManager.getInstance()
        try {
            Log.i(TAG, "Configuring GPIO pins")
            ledGpio = pioService.openGpio(GPIO_PIN_LED)
            ledGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

            buzzerGpio = pioService.openGpio(GPIO_PIN_BUZZER)
            buzzerGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

            sensorGpio = pioService.openGpio(GPIO_PIN_SENSOR)
            sensorGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

            Log.i(TAG, "Configuring successful")
        } catch (e: IOException) {
            Log.e(TAG, "Error configuring GPIO pins", e)
        }
    }

    private fun setUpModules() {
        buzzer = Buzzer(buzzerGpio)
        led = Led(ledGpio)
        detector = Detector(sensorGpio, this)
    }

    private fun setUpFirebase() {
        attackRef = database.getReference(ATTACK_REF)
        attackRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot?) {
                if (data?.value != null) {
                    if (data.value as Boolean) {
                        buzzer.on()
                        led.on()
                    } else {
                        buzzer.off()
                        led.off()
                    }
                }
            }

            override fun onCancelled(data: DatabaseError?) {}
        })

        detectorActivatedRef = database.getReference(DETECTOR_ACTIVATED_REF)
        detectorActivatedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot?) {
                if (data?.value != null) {
                    if (data.value as Boolean) {
                        detector.on()
                    } else {
                        detector.off()
                    }
                }
            }
            override fun onCancelled(data: DatabaseError?) {}
        })

    }

    private fun initCamera() {
        // Creates new handlers and associated threads for camera
        cameraThread = HandlerThread(CAMERA_THREAD_NAME)
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
        val dbImage = database.getReference(IMAGE_NAME_REF)
        dbImage.setValue(Base64.encodeToString(imageBytes, Base64.DEFAULT))
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
