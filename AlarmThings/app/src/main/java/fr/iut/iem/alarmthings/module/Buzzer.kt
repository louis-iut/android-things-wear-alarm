package fr.iut.iem.alarmthings.module

import com.google.android.things.pio.Gpio
import kotlin.concurrent.thread

class Buzzer(private val buzzerGpio: Gpio) : BaseModule() {
    companion object {
        private const val THREAD_NAME = "Buzzer"
    }

    override fun createThread() {
        thread(true, true, null, THREAD_NAME, 0, { buzz() })
    }

    private fun buzz() {
        while (run) {
            buzzerGpio.value = true
            Thread.sleep(1000)
            buzzerGpio.value = false
            Thread.sleep(100)
        }

        buzzerGpio.value = false
    }
}