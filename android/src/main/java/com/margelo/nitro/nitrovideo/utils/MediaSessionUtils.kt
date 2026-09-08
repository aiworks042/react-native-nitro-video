package com.margelo.nitro.nitrovideo.utils

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession

/**
 * Creates a basic foreground media session, for receiving commands like play/pause from bluetooth devices.
 */
@OptIn(UnstableApi::class)
fun buildBasicMediaSession(context: Context, player: ExoPlayer): MediaSession {
    return MediaSession.Builder(context, player)
        .setId("NitroVideoBasicMediaSession_${player.hashCode()}")
        .build()
}
