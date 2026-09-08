package com.margelo.nitro.nitrovideo.utils

import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.events.EventDispatcher
import com.facebook.react.uimanager.events.TouchEvent
import com.facebook.react.uimanager.events.TouchEventCoalescingKeyHelper
import com.facebook.react.uimanager.events.TouchEventType

internal fun EventDispatcher.dispatchMotionEvent(
    view: View,
    event: MotionEvent?,
    touchEventCoalescingKeyHelper: TouchEventCoalescingKeyHelper
) {
    if (event == null) return

    try {
        val touchEvent = TouchEvent.obtain(
            UIManagerHelper.getSurfaceId(view),
            view.id,
            event.toTouchEventType(),
            event,
            event.eventTime,
            event.x,
            event.y,
            touchEventCoalescingKeyHelper
        )
        dispatchEvent(touchEvent)
    } catch (e: RuntimeException) {
        Log.e("EventDispatcherUtils", "Error dispatching touch event", e)
    }
}

private fun MotionEvent.toTouchEventType(): TouchEventType {
    return when (this.actionMasked) {
        MotionEvent.ACTION_DOWN -> TouchEventType.START
        MotionEvent.ACTION_UP -> TouchEventType.END
        MotionEvent.ACTION_MOVE -> TouchEventType.MOVE
        MotionEvent.ACTION_CANCEL -> TouchEventType.CANCEL
        else -> TouchEventType.CANCEL
    }
}
