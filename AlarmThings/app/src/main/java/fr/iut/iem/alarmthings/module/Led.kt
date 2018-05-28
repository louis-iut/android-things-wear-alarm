package fr.iut.iem.alarmthings.module

import com.google.android.things.pio.Gpio
import kotlin.concurrent.thread

class Led(private val ledGpio: Gpio): BaseModule() {
    companion object {
        private const val THREAD_NAME = "Led"
    }

    override fun createThread() {
        thread(true, true, null, THREAD_NAME, 0, { blink() })
    }

    private fun blink() {
        while (run) {
            ledGpio.value = true
            Thread.sleep(1000)
            ledGpio.value = false
            Thread.sleep(1000)
        }

        ledGpio.value = false
    }
}