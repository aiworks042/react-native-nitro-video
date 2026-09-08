package com.margelo.nitro.nitrovideo.records

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ScrubbingModeParameters
import java.io.Serializable

@OptIn(UnstableApi::class)
class ScrubbingModeOptions(
    var scrubbingModeEnabled: Boolean = false,
    var increaseCodecOperatingRate: Boolean = true,
    var enableDynamicScheduling: Boolean = true,
    var useDecodeOnlyFlag: Boolean = true,
    var allowSkippingMediaCodecFlush: Boolean = true
) : Serializable {
    fun applyToPlayer(player: ExoPlayer) {
        player.isScrubbingModeEnabled = scrubbingModeEnabled
        player.scrubbingModeParameters = toScrubbingModeParameters()
    }

    private fun toScrubbingModeParameters(): ScrubbingModeParameters {
        return ScrubbingModeParameters.Builder()
            .setUseDecodeOnlyFlag(useDecodeOnlyFlag)
            .setAllowSkippingMediaCodecFlush(allowSkippingMediaCodecFlush)
            .setShouldIncreaseCodecOperatingRate(increaseCodecOperatingRate)
            .setShouldEnableDynamicScheduling(enableDynamicScheduling)
            .build()
    }
}
