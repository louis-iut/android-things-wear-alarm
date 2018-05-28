package fr.iut.iem.alarmthings.module

import android.util.Log
import com.google.android.things.pio.Gpio
import kotlin.concurrent.thread

interface DetectorListener {
    fun onDetect()
}

class Detector(private val sensorGpio: Gpio, private val listener: DetectorListener) : BaseModule() {

    companion object {
        private const val THREAD_NAME = "Detector"
    }

    private var isDetected = false

    override fun createThread() {
        thread(true, true, null, THREAD_NAME, 0, { detect() })
    }

    private fun detect() {
        while (run) {
            Thread.sleep(100)

            if (sensorGpio.value && !isDetected) {
                isDetected = true
                listener.onDetect()
            }

            if (!sensorGpio.value && isDetected) {
                isDetected = false
            }
        }
    }
}
