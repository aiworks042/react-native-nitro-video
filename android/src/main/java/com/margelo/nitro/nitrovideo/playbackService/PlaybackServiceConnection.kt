package com.margelo.nitro.nitrovideo.playbackService

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.margelo.nitro.nitrovideo.HybridVideoPlayer
import java.lang.ref.WeakReference

@OptIn(UnstableApi::class)
class PlaybackServiceConnection(val player: WeakReference<HybridVideoPlayer>) : ServiceConnection {
    var playbackServiceBinder: PlaybackServiceBinder? = null
        private set
    var isConnected = false
        private set

    override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
        val player = player.get() ?: return
        val serviceBinder = binder as? PlaybackServiceBinder ?: run {
            Log.e("NitroVideo", "Could not bind to playback service: invalid binder")
            return
        }

        isConnected = true
        playbackServiceBinder = serviceBinder
        serviceBinder.service.registerPlayer(player)
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
        playbackServiceBinder = null
        isConnected = false
    }

    override fun onBindingDied(name: ComponentName?) {
        isConnected = false
        Log.e("NitroVideo", "Lost connection to the playback service binder")
        super.onBindingDied(name)
    }

    override fun onNullBinding(componentName: ComponentName) {
        isConnected = false
        Log.e("NitroVideo", "Null binding on playback service")
        super.onNullBinding(componentName)
    }
}
