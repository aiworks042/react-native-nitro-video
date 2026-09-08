package com.margelo.nitro.nitrovideo.records

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import java.io.Serializable

@OptIn(UnstableApi::class)
class SeekTolerance(
    var toleranceBefore: Double = 0.0,
    var toleranceAfter: Double = 0.0
) : Serializable {
    fun applyToPlayer(player: ExoPlayer) {
        player.setSeekParameters(toSeekParameters())
    }

    private fun toSeekParameters(): SeekParameters {
        val toleranceBeforeMs = (toleranceBefore * 1000).toLong()
        val toleranceAfterMs = (toleranceAfter * 1000).toLong()
        return SeekParameters(
            Util.msToUs(toleranceBeforeMs),
            Util.msToUs(toleranceAfterMs)
        )
    }
}
