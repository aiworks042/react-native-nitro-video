package com.margelo.nitro.nitrovideo.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Track data models
// ─────────────────────────────────────────────────────────────────────────────

data class NativeTrackInfo(
    val id: String,
    val language: String,
    val label: String,
    val isDefault: Boolean,
    val autoSelect: Boolean
)

// ─────────────────────────────────────────────────────────────────────────────
// VideoPlayerTracksHelper — manages ExoPlayer track selection & switching
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
class VideoPlayerTracksHelper(private val player: ExoPlayer) {

    private val subtitleFormatsToGroups = mutableMapOf<Format, Pair<TrackGroup, Int>>()
    private val audioFormatsToGroups = mutableMapOf<Format, Pair<TrackGroup, Int>>()

    val availableSubtitleTracks = mutableListOf<NativeTrackInfo>()
    val availableAudioTracks = mutableListOf<NativeTrackInfo>()

    fun onTracksChanged(tracks: Tracks) {
        subtitleFormatsToGroups.clear()
        availableSubtitleTracks.clear()
        audioFormatsToGroups.clear()
        availableAudioTracks.clear()

        for (group in tracks.groups) {
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val id = format.id ?: "$i"
                val lang = format.language ?: "und"
                val label = format.label ?: Locale(lang).displayLanguage
                val isDef = (format.selectionFlags and C.SELECTION_FLAG_DEFAULT) != 0
                val autoSel = (format.selectionFlags and C.SELECTION_FLAG_AUTOSELECT) != 0

                val trackInfo = NativeTrackInfo(id, lang, label, isDef, autoSel)

                if (MimeTypes.isText(format.sampleMimeType)) {
                    subtitleFormatsToGroups[format] = Pair(group.mediaTrackGroup, i)
                    availableSubtitleTracks.add(trackInfo)
                } else if (MimeTypes.isAudio(format.sampleMimeType)) {
                    audioFormatsToGroups[format] = Pair(group.mediaTrackGroup, i)
                    availableAudioTracks.add(trackInfo)
                }
            }
        }
    }

    fun selectSubtitleTrack(trackId: String) {
        var params = player.trackSelectionParameters
        params = params.buildUpon().clearOverridesOfType(C.TRACK_TYPE_TEXT).build()

        if (trackId.isEmpty()) {
            player.trackSelectionParameters = params.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
            return
        }

        val match = subtitleFormatsToGroups.entries.firstOrNull { (format, _) ->
            format.id == trackId || format.language == trackId
        }

        if (match != null) {
            val (trackGroup, index) = match.value
            val override = TrackSelectionOverride(trackGroup, index)
            player.trackSelectionParameters = params.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .addOverride(override)
                .build()
        }
    }

    fun selectAudioTrack(trackId: String) {
        var params = player.trackSelectionParameters
        params = params.buildUpon().clearOverridesOfType(C.TRACK_TYPE_AUDIO).build()

        if (trackId.isEmpty()) {
            return
        }

        val match = audioFormatsToGroups.entries.firstOrNull { (format, _) ->
            format.id == trackId || format.language == trackId
        }

        if (match != null) {
            val (trackGroup, index) = match.value
            val override = TrackSelectionOverride(trackGroup, index)
            player.trackSelectionParameters = params.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .addOverride(override)
                .build()
        }
    }

    fun setMaxResolution(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setMaxVideoSize(width, height)
                .build()
        } else {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .clearVideoSizeConstraints()
                .build()
        }
    }
}
