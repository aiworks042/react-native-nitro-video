package com.margelo.nitro.nitrovideo

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import com.margelo.nitro.nitrovideo.records.ButtonOptions

@OptIn(UnstableApi::class)
internal fun PlayerView.applyRequiresLinearPlayback(requireLinearPlayback: Boolean) {
    setShowFastForwardButton(!requireLinearPlayback)
    setShowRewindButton(!requireLinearPlayback)
    setShowPreviousButton(!requireLinearPlayback)
    setShowNextButton(!requireLinearPlayback)
    setTimeBarInteractive(requireLinearPlayback)
}

@OptIn(UnstableApi::class)
internal fun PlayerView.setTimeBarInteractive(interactive: Boolean) {
    val timeBar = findViewById<DefaultTimeBar>(androidx.media3.ui.R.id.exo_progress)
    if (interactive) {
        timeBar?.setScrubberColor(Color.TRANSPARENT)
        timeBar?.isEnabled = false
    } else {
        timeBar?.setScrubberColor(Color.WHITE)
        timeBar?.isEnabled = true
    }
}

@OptIn(UnstableApi::class)
internal fun PlayerView.setFullscreenButtonVisibility(visible: Boolean) {
    val fullscreenButton = findViewById<ImageButton>(androidx.media3.ui.R.id.exo_fullscreen)
    fullscreenButton?.visibility = if (visible) View.VISIBLE else View.GONE
}

@OptIn(UnstableApi::class)
internal fun PlayerView.setSettingsButtonVisibility(visible: Boolean) {
    val settingsButton = findViewById<ImageButton>(androidx.media3.ui.R.id.exo_settings)
    settingsButton?.visibility = if (visible) View.VISIBLE else View.GONE
}

@OptIn(UnstableApi::class)
internal fun PlayerView.setPlayPauseButtonVisibility(visible: Boolean) {
    val playPauseButton = findViewById<ImageButton>(androidx.media3.ui.R.id.exo_play_pause)
    playPauseButton?.visibility = if (visible) View.VISIBLE else View.GONE
}

@OptIn(UnstableApi::class)
internal fun PlayerView.setBottomBarVisibility(visible: Boolean) {
    val bottomBar = findViewById<ViewGroup>(androidx.media3.ui.R.id.exo_bottom_bar)
    bottomBar?.visibility = if (visible) View.VISIBLE else View.GONE

    val progressBar = findViewById<DefaultTimeBar>(androidx.media3.ui.R.id.exo_progress)
    progressBar?.visibility = if (visible) View.VISIBLE else View.GONE
}

@OptIn(UnstableApi::class)
internal fun PlayerView.applyButtonOptions(
    config: ButtonOptions,
    requiresLinearPlayback: Boolean = false
) {
    setShowFastForwardButton(!requiresLinearPlayback && config.showSeekForward)
    setShowRewindButton(!requiresLinearPlayback && config.showSeekBackward)
    setShowPreviousButton(!requiresLinearPlayback && config.showPrevious)
    setShowNextButton(!requiresLinearPlayback && config.showNext)
    setSettingsButtonVisibility(config.showSettings)
    setPlayPauseButtonVisibility(config.showPlayPause)
    setBottomBarVisibility(config.showBottomBar)
}
