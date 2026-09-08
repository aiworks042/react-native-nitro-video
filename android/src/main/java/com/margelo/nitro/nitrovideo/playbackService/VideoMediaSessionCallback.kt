package com.margelo.nitro.nitrovideo.playbackService

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class VideoMediaSessionCallback : MediaSession.Callback {
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        try {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(
                    MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                        .add(Player.COMMAND_SEEK_FORWARD)
                        .add(Player.COMMAND_SEEK_BACK)
                        .build()
                )
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .add(SessionCommand(NitroVideoPlaybackService.SEEK_BACKWARD_COMMAND, Bundle.EMPTY))
                        .add(SessionCommand(NitroVideoPlaybackService.SEEK_FORWARD_COMMAND, Bundle.EMPTY))
                        .build()
                )
                .build()
        } catch (_: Exception) {
            return MediaSession.ConnectionResult.reject()
        }
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            NitroVideoPlaybackService.SEEK_FORWARD_COMMAND ->
                session.player.seekTo(session.player.currentPosition + NitroVideoPlaybackService.SEEK_INTERVAL_MS)
            NitroVideoPlaybackService.SEEK_BACKWARD_COMMAND ->
                session.player.seekTo(session.player.currentPosition - NitroVideoPlaybackService.SEEK_INTERVAL_MS)
        }
        return super.onCustomCommand(session, controller, customCommand, args)
    }
}
