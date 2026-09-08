package com.margelo.nitro.nitrovideo.utils

import android.os.Bundle
import androidx.fragment.app.Fragment
import java.lang.ref.WeakReference
import java.util.UUID

interface PictureInPictureFragmentListener {
    fun onPictureInPictureStart()
    fun onPictureInPictureStop()
}

// Keep this fragment restorable after Android process death.
class PictureInPictureHelperFragment() : Fragment() {
    constructor(listener: PictureInPictureFragmentListener) : this() {
        this.listener = WeakReference(listener)
    }

    val id = "${PictureInPictureHelperFragment::class.java.simpleName}_${UUID.randomUUID()}"
    private var listener: WeakReference<PictureInPictureFragmentListener>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (listener == null) {
            parentFragmentManager
                .beginTransaction()
                .remove(this)
                .commitAllowingStateLoss()
        }
    }

    fun release() {
        listener?.clear()
        listener = null
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            listener?.get()?.onPictureInPictureStart()
        } else {
            listener?.get()?.onPictureInPictureStop()
        }
    }
}
