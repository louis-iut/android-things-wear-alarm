package fr.iut.iem.alarmthings

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
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
    }

    private lateinit var ledGpio: Gpio
    private lateinit var sensorGpio: Gpio
    private lateinit var buzzerGpio: Gpio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpGpio()
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
                if (sensorGpio.value) {
                    Log.i(TAG, sensorGpio.value.toString())
                    buzz()
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
                sleep(300)
                // Turn off the LED
                ledGpio.value = false
                sleep(300)
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
}
