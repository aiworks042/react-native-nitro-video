package com.margelo.nitro.nitrovideo

import android.os.Handler
import android.os.Looper
import com.margelo.nitro.nitrovideo.delegates.IgnoreSameSet
import java.lang.ref.WeakReference

fun interface IntervalUpdateEmitter {
    fun emitTimeUpdate()
}

class IntervalUpdateClock(emitter: IntervalUpdateEmitter) {
    private val emitter = WeakReference(emitter)
    private var handler = Handler(Looper.getMainLooper())

    var interval by IgnoreSameSet(0L) { new: Long, _: Long? ->
        if (new <= 0) {
            stop()
        } else {
            startOrUpdate()
        }
    }

    private var isRunning = false

    private fun stop() {
        handler.removeCallbacksAndMessages(null)
        isRunning = false
    }

    private fun startOrUpdate() {
        if (!isRunning) {
            emitter.get()?.emitTimeUpdate()
        } else {
            handler.removeCallbacksAndMessages(null)
        }
        isRunning = true
        scheduleNextUpdate()
    }

    private fun scheduleNextUpdate() {
        if (interval <= 0L) return

        val update = {
            emitter.get()?.emitTimeUpdate()
            scheduleNextUpdate()
        }

        handler.postDelayed(update, interval)
    }
}
