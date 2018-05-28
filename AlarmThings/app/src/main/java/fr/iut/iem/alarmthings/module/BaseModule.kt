package fr.iut.iem.alarmthings.module

abstract class BaseModule {
    protected var run = false

    fun on() {
        if (!run) {
            run = true
            createThread()
        }
    }

    fun off() {
        run = false
    }

    fun isOn(): Boolean {
        return run
    }

    protected abstract fun createThread()
}