package com.margelo.nitro.nitrovideo.playbackService

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionCommand
import com.google.common.collect.ImmutableList
import com.margelo.nitro.nitrovideo.HybridVideoPlayer

class PlaybackServiceBinder(val service: NitroVideoPlaybackService) : Binder()

@OptIn(UnstableApi::class)
class NitroVideoPlaybackService : MediaSessionService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mediaSessions = mutableMapOf<ExoPlayer, MediaSession>()
    private val binder = PlaybackServiceBinder(this)
    private var mostRecentInteractionSession: MediaSession? = null
    private var isForeground: Boolean = false

    private val commandSeekForward = SessionCommand(SEEK_FORWARD_COMMAND, Bundle.EMPTY)
    private val commandSeekBackward = SessionCommand(SEEK_BACKWARD_COMMAND, Bundle.EMPTY)
    private val seekForwardButton = CommandButton.Builder()
        .setDisplayName("forward")
        .setSessionCommand(commandSeekForward)
        .setIconResId(androidx.media3.session.R.drawable.media3_icon_circular_play)
        .build()

    private val seekBackwardButton = CommandButton.Builder()
        .setDisplayName("rewind")
        .setSessionCommand(commandSeekBackward)
        .setIconResId(androidx.media3.session.R.drawable.media3_icon_circular_play)
        .build()

    fun setShowNotification(showNotification: Boolean, player: ExoPlayer) {
        mainHandler.post {
            val sessionExtras = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mediaSessions[player]?.sessionExtras?.deepCopy() ?: Bundle()
            } else {
                Bundle()
            }
            sessionExtras.putBoolean(SESSION_SHOW_NOTIFICATION, showNotification)
            mediaSessions[player]?.let {
                it.sessionExtras = sessionExtras
                onUpdateNotification(it, showNotification && player.playWhenReady)
            }
        }
    }

    fun registerPlayer(videoPlayer: HybridVideoPlayer) {
        mainHandler.post {
            val player = videoPlayer.exoPlayer ?: return@post
            if (mediaSessions[player] != null) return@post

            val mediaSession = MediaSession.Builder(this@NitroVideoPlaybackService, player)
                .setId("NitroVideoPlaybackService_${player.hashCode()}")
                .setCallback(VideoMediaSessionCallback())
                .setCustomLayout(ImmutableList.of(seekBackwardButton, seekForwardButton))
                .build()

            videoPlayer.mediaSession?.release()
            videoPlayer.mediaSession = mediaSession

            mediaSessions[player] = mediaSession
            addSession(mediaSession)
            setShowNotification(videoPlayer.showNowPlayingNotification, player)
        }
    }

    fun unregisterPlayer(player: ExoPlayer) {
        mainHandler.post {
            hidePlayerNotification(player)
            val session = mediaSessions.remove(player)
            session?.release()
            if (mediaSessions.isEmpty()) {
                cleanup()
                stopSelf()
            } else {
                setMostRecentInteractionSession(findMostRecentInteractionSession())
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        mainHandler.post {
            if (startInForegroundRequired && session.wantsToShowNotification()) {
                setMostRecentInteractionSession(session)
            } else {
                setMostRecentInteractionSession(findMostRecentInteractionSession())
            }
        }
    }

    @MainThread
    private fun setMostRecentInteractionSession(session: MediaSession?) {
        if (session?.player?.playWhenReady == false) {
            stopForeground(STOP_FOREGROUND_DETACH)
            isForeground = false
        }

        if (mostRecentInteractionSession != session) {
            hideAllNotifications()
        }

        mostRecentInteractionSession = session
        session?.let {
            createNotification(it, it.player.playWhenReady)
        } ?: run {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
    }

    @MainThread
    private fun findMostRecentInteractionSession(): MediaSession? {
        val prioritizedSessions = (listOfNotNull(mostRecentInteractionSession) + mediaSessions.values.toList()).distinct()
        return prioritizedSessions.firstOrNull { it.wantsToShowNotification() && it.player.playWhenReady }
            ?: prioritizedSessions.firstOrNull { it.wantsToShowNotification() }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        cleanup()
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = null

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    @MainThread
    private fun createNotification(session: MediaSession, startInForegroundRequired: Boolean = false) {
        if (session.player.currentMediaItem == null) return

        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_LOW)
            )
        }

        val contentTitle = session.player.currentMediaItem?.mediaMetadata?.title ?: "\u200E"
        val notificationCompat = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(androidx.media3.session.R.drawable.media3_icon_circular_play)
            .setContentTitle(contentTitle)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(session))
            .build()

        val notificationId = session.player.hashCode()

        if (startInForegroundRequired) {
            try {
                startForeground(notificationId, notificationCompat)
                isForeground = true
            } catch (e: Exception) {
                Log.e("NitroVideo", "Failed to start foreground playback service", e)
            }
        } else {
            notificationManager.notify(notificationId, notificationCompat)
        }
    }

    private fun cleanup() {
        mainHandler.post {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
            isForeground = false

            hideAllNotifications()

            val sessionsToRelease = mediaSessions.values.toList()
            mediaSessions.clear()
            for (session in sessionsToRelease) {
                session.release()
            }

            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.deleteNotificationChannel(CHANNEL_ID)
            }
        }
    }

    @MainThread
    private fun hidePlayerNotification(player: ExoPlayer) {
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(player.hashCode())
    }

    @MainThread
    private fun hideAllNotifications() {
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    private fun MediaSession.wantsToShowNotification(): Boolean =
        this.sessionExtras.getBoolean(SESSION_SHOW_NOTIFICATION, false)

    companion object {
        const val SEEK_FORWARD_COMMAND = "SEEK_FORWARD"
        const val SEEK_BACKWARD_COMMAND = "SEEK_REWIND"
        const val CHANNEL_ID = "NitroVideoPlaybackService"
        const val SESSION_SHOW_NOTIFICATION = "showNotification"
        const val SEEK_INTERVAL_MS = 10000L

        fun startService(context: Context, serviceConnection: PlaybackServiceConnection): Boolean {
            val intent = Intent(context, NitroVideoPlaybackService::class.java)
            intent.action = SERVICE_INTERFACE
            context.startService(intent)

            val flags = if (Build.VERSION.SDK_INT >= 29) {
                BIND_AUTO_CREATE or BIND_INCLUDE_CAPABILITIES
            } else {
                BIND_AUTO_CREATE
            }
            return context.bindService(intent, serviceConnection, flags)
        }
    }
}
